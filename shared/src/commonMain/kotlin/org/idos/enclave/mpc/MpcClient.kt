package org.idos.enclave.mpc

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.idos.enclave.EnclaveError
import org.idos.enclave.MpcNodeFailure
import org.idos.enclave.crypto.Encryption
import org.idos.enclave.crypto.ShamirSharing
import org.idos.enclave.crypto.ShareBlinding
import org.idos.kwil.types.HexString
import org.idos.logging.HttpLogLevel
import org.idos.logging.IdosLogger

/**
 * Configuration for MPC operations.
 *
 * @param partisiaRpcUrl URL of the Partisia blockchain RPC endpoint
 * @param contractAddress Address of the MPC contract (hex string)
 * @param totalNodes Total number of MPC nodes (n)
 * @param threshold Minimum number of shares needed to reconstruct secret (k)
 * @param maliciousNodes Number of potentially malicious nodes to tolerate
 */
data class MpcConfig(
    val partisiaRpcUrl: String,
    val contractAddress: HexString,
    val totalNodes: Int,
    val threshold: Int,
    val maliciousNodes: Int = 0,
) {
    init {
        require(threshold <= totalNodes) { "Threshold must be <= total nodes" }
        require(threshold > 0) { "Threshold must be > 0" }
        require(maliciousNodes >= 0) { "Malicious nodes must be >= 0" }
    }

    val minSuccessfulNodes: Int get() = threshold + maliciousNodes
}

/**
 * Internal sealed class to track node operation results.
 */
private sealed class NodeResult {
    data class Success(
        val nodeIndex: Int,
        val share: ByteArray,
    ) : NodeResult()

    data class Failure(
        val nodeIndex: Int,
        val error: Throwable,
    ) : NodeResult()
}

/**
 * Internal MPC client that coordinates secret sharing across multiple nodes.
 *
 * This client handles:
 * - Splitting secrets using Shamir's Secret Sharing
 * - Distributing shares to multiple MPC nodes
 * - Reconstructing secrets from distributed shares
 * - Managing recovery addresses across nodes
 *
 * Authentication is handled externally - this client accepts pre-signed signatures.
 * Node discovery is on-demand - fetches fresh node list for each operation.
 *
 * @param encryption Encryption instance for decrypting downloaded shares
 * @param config MPC configuration (URL, contract, nodes, threshold, etc.)
 * @param httpLogLevel Log level for HTTP requests/responses
 */
internal class MpcClient(
    private val encryption: Encryption,
    val config: MpcConfig,
    private val httpLogLevel: HttpLogLevel = HttpLogLevel.NONE,
) {
    private val rpcClient = PartisiaRpcClient(config.partisiaRpcUrl, config.contractAddress)

    /**
     * Fetch current node clients from blockchain on-demand.
     * Creates fresh clients for each operation - no caching.
     */
    private suspend fun getNodeClients(): List<NodeClient> {
        val nodeConfigs = rpcClient.getState()

        if (nodeConfigs.size != config.totalNodes) {
            throw EnclaveError.MpcNotEnoughNodes("Expected ${config.totalNodes} nodes but found ${nodeConfigs.size}")
        }

        return nodeConfigs.map { node ->
            NodeClient(
                baseUrl = node.url,
                contractAddress = config.contractAddress,
                httpLogLevel = httpLogLevel,
            )
        }
    }

    /**
     * Upload a secret by splitting it into shares and distributing to nodes.
     *
     * @param id Unique identifier for this secret
     * @param request Upload request with commitments and recovery addresses
     * @param signature Pre-signed authorization signature
     * @param blindedShares The blinded shares to upload (one per node)
     * @throws EnclaveError.MpcUploadFailed if insufficient nodes succeeded
     */
    suspend fun uploadSecret(
        id: String,
        request: UploadRequest,
        signature: String,
        blindedShares: List<ByteArray>,
    ): Unit =
        coroutineScope {
            require(blindedShares.size == config.totalNodes) {
                "Must provide ${config.totalNodes} blinded shares, got ${blindedShares.size}"
            }

            // Fetch fresh node clients on-demand
            val clients = getNodeClients()

            // Create upload sharing data for each node
            val sharingRequests =
                blindedShares.mapIndexed { index, blindedShare ->
                    Sharing(
                        shareCommitments = request.shareCommitments,
                        recoveringAddresses = request.recoveringAddresses,
                        shareData = blindedShare.toHexString(),
                    )
                }

            // Upload to all nodes in parallel, tracking both successes and failures
            val results =
                clients
                    .mapIndexed { index, client ->
                        async {
                            try {
                                client.sendUpload(id, sharingRequests[index], signature)
                                NodeResult.Success(index, ByteArray(0))
                            } catch (e: Exception) {
                                IdosLogger.d("MPC") { "Node $index upload failed: ${e.message}" }
                                NodeResult.Failure(index, e)
                            }
                        }
                    }.awaitAll()

            val successes = results.filterIsInstance<NodeResult.Success>()
            val failures = results.filterIsInstance<NodeResult.Failure>()

            if (successes.size < config.minSuccessfulNodes) {
                throw EnclaveError.MpcUploadFailed(
                    successCount = successes.size,
                    required = config.minSuccessfulNodes,
                    failures = failures.map { MpcNodeFailure(it.nodeIndex, it.error) },
                )
            }
        }

    /**
     * Download and reconstruct a secret from distributed shares.
     *
     * @param id Unique identifier of the secret
     * @param request Download request with recovery address and ephemeral public key
     * @param signature Pre-signed authorization signature
     * @param ephemeralSecretKey Secret key for decrypting shares (32 bytes)
     * @return The reconstructed secret
     * @throws EnclaveError.MpcNotEnoughShares if insufficient shares were obtained
     */
    suspend fun downloadSecret(
        id: String,
        request: DownloadRequest,
        signature: String,
        ephemeralSecretKey: ByteArray,
    ): ByteArray =
        coroutineScope {
            // Fetch fresh node clients on-demand
            val clients = getNodeClients()

            // Download from all nodes in parallel, tracking both successes and failures
            val results =
                clients
                    .mapIndexed { index, client ->
                        async {
                            try {
                                val encryptedShare = client.sendDownload(id, request, signature)

                                // Decrypt share using NaCl box
                                val nonce = encryptedShare.nonce.removePrefix("0x").hexToByteArray()
                                val ciphertext = encryptedShare.encryptedShare.removePrefix("0x").hexToByteArray()
                                val nodePubkey = encryptedShare.publicKey.removePrefix("0x").hexToByteArray()
                                val fullMessage = nonce + ciphertext

                                // Decrypt with ephemeral secret key
                                val decryptedBlindedShare = encryption.decrypt(fullMessage, ephemeralSecretKey, nodePubkey)

                                // Remove blinding
                                val share = ShareBlinding.unblind(decryptedBlindedShare)

                                NodeResult.Success(index, share)
                            } catch (e: Exception) {
                                IdosLogger.d("MPC") { "Node $index download failed: ${e.message}" }
                                NodeResult.Failure(index, e)
                            }
                        }
                    }.awaitAll()

            val successes = results.filterIsInstance<NodeResult.Success>()
            val failures = results.filterIsInstance<NodeResult.Failure>()

            if (successes.size < config.threshold) {
                throw EnclaveError.MpcNotEnoughShares(
                    obtained = successes.size,
                    required = config.threshold,
                    failures = failures.map { MpcNodeFailure(it.nodeIndex, it.error) },
                )
            }

            // Reconstruct secret from shares using Shamir's Secret Sharing
            ShamirSharing.combineByteWiseShamir(
                successes.map { it.share },
                config.threshold,
            )
        }

    /**
     * Add a new recovery address to an existing secret.
     *
     * @param id Unique identifier of the secret
     * @param request Add address request
     * @param signature Pre-signed authorization signature
     * @throws EnclaveError.MpcManagementFailed if insufficient nodes succeeded
     */
    suspend fun addAddress(
        id: String,
        request: AddAddressRequest,
        signature: String,
    ): Unit =
        coroutineScope {
            // Fetch fresh node clients on-demand
            val clients = getNodeClients()

            // Send to all nodes in parallel, tracking both successes and failures
            val results =
                clients
                    .mapIndexed { index, client ->
                        async {
                            try {
                                client.sendAddAddress(id, request, signature)
                                NodeResult.Success(index, ByteArray(0))
                            } catch (e: Exception) {
                                IdosLogger.d("MPC") { "Node $index add address failed: ${e.message}" }
                                NodeResult.Failure(index, e)
                            }
                        }
                    }.awaitAll()

            val successes = results.filterIsInstance<NodeResult.Success>()
            val failures = results.filterIsInstance<NodeResult.Failure>()

            if (successes.size < config.minSuccessfulNodes) {
                throw EnclaveError.MpcManagementFailed(
                    operation = "add address",
                    successCount = successes.size,
                    required = config.minSuccessfulNodes,
                    failures = failures.map { MpcNodeFailure(it.nodeIndex, it.error) },
                )
            }
        }

    /**
     * Remove a recovery address from an existing secret.
     *
     * @param id Unique identifier of the secret
     * @param request Remove address request
     * @param signature Pre-signed authorization signature
     * @throws EnclaveError.MpcManagementFailed if insufficient nodes succeeded
     */
    suspend fun removeAddress(
        id: String,
        request: RemoveAddressRequest,
        signature: String,
    ): Unit =
        coroutineScope {
            // Fetch fresh node clients on-demand
            val clients = getNodeClients()

            // Send to all nodes in parallel, tracking both successes and failures
            val results =
                clients
                    .mapIndexed { index, client ->
                        async {
                            try {
                                client.sendRemoveAddress(id, request, signature)
                                NodeResult.Success(index, ByteArray(0))
                            } catch (e: Exception) {
                                IdosLogger.d("MPC") { "Node $index remove address failed: ${e.message}" }
                                NodeResult.Failure(index, e)
                            }
                        }
                    }.awaitAll()

            val successes = results.filterIsInstance<NodeResult.Success>()
            val failures = results.filterIsInstance<NodeResult.Failure>()

            if (successes.size < config.minSuccessfulNodes) {
                throw EnclaveError.MpcManagementFailed(
                    operation = "remove address",
                    successCount = successes.size,
                    required = config.minSuccessfulNodes,
                    failures = failures.map { MpcNodeFailure(it.nodeIndex, it.error) },
                )
            }
        }

    /**
     * Update the list of wallet addresses for a secret.
     *
     * @param id Unique identifier of the secret
     * @param request Update wallets request
     * @param signature Pre-signed authorization signature
     * @throws EnclaveError.MpcManagementFailed if insufficient nodes succeeded
     */
    suspend fun updateWallets(
        id: String,
        request: UpdateWalletsRequest,
        signature: String,
    ): Unit =
        coroutineScope {
            // Fetch fresh node clients on-demand
            val clients = getNodeClients()

            // Send to all nodes in parallel, tracking both successes and failures
            val results =
                clients
                    .mapIndexed { index, client ->
                        async {
                            try {
                                client.sendUpdate(id, request, signature)
                                NodeResult.Success(index, ByteArray(0))
                            } catch (e: Exception) {
                                IdosLogger.d("MPC") { "Node $index update wallets failed: ${e.message}" }
                                NodeResult.Failure(index, e)
                            }
                        }
                    }.awaitAll()

            val successes = results.filterIsInstance<NodeResult.Success>()
            val failures = results.filterIsInstance<NodeResult.Failure>()

            if (successes.size < config.minSuccessfulNodes) {
                throw EnclaveError.MpcManagementFailed(
                    operation = "update wallets",
                    successCount = successes.size,
                    required = config.minSuccessfulNodes,
                    failures = failures.map { MpcNodeFailure(it.nodeIndex, it.error) },
                )
            }
        }
}
