package org.idos.enclave.mpc

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.idos.crypto.Keccak256Hasher
import org.idos.enclave.EnclaveError
import org.idos.enclave.crypto.Encryption
import org.idos.enclave.crypto.ShamirSharing
import org.idos.enclave.crypto.ShareBlinding
import org.idos.kwil.types.HexString

/**
 * Configuration for MPC operations.
 *
 * @param totalNodes Total number of MPC nodes (n)
 * @param threshold Minimum number of shares needed to reconstruct secret (k)
 * @param maliciousNodes Number of potentially malicious nodes to tolerate
 */
data class MpcConfig(
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
 * Result of an upload operation.
 *
 * @param successCount Number of successful uploads
 * @param totalNodes Total number of nodes attempted
 * @param successful Whether the operation met the minimum success threshold
 */
data class UploadResult(
    val successCount: Int,
    val totalNodes: Int,
    val successful: Boolean,
)

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
 *
 * @param partisiaRpcUrl URL of the Partisia blockchain RPC endpoint
 * @param contractAddress Address of the MPC contract
 * @param encryption Encryption instance for decrypting downloaded shares
 * @param hasher Keccak256 hasher for computing share commitments
 * @param config MPC configuration (nodes, threshold, etc.)
 */
internal class MpcClient(
    private val partisiaRpcUrl: String,
    private val contractAddress: HexString,
    private val encryption: Encryption,
    private val hasher: Keccak256Hasher,
    val config: MpcConfig,
) {
    private val rpcClient = PartisiaRpcClient(partisiaRpcUrl, contractAddress)
    internal var nodeClients: List<NodeClient> = emptyList()

    /**
     * Initialize the client by discovering MPC nodes from the blockchain.
     */
    suspend fun initialize() {
        val nodeConfigs = rpcClient.getState()

        if (nodeConfigs.size != config.totalNodes) {
            throw EnclaveError.MpcNotEnoughNodes("Expected ${config.totalNodes} nodes but found ${nodeConfigs.size}")
        }

        nodeClients =
            nodeConfigs.map { node ->
                NodeClient(
                    baseUrl = node.url,
                    contractAddress = contractAddress,
                )
            }
    }

    private fun requireInitialized() {
        if (nodeClients.isEmpty()) throw EnclaveError.MpcNotInitialized()
    }

    /**
     * Upload a secret by splitting it into shares and distributing to nodes.
     *
     * @param id Unique identifier for this secret
     * @param request Upload request with commitments and recovery addresses
     * @param signature Pre-signed authorization signature
     * @param blindedShares The blinded shares to upload (one per node)
     * @return UploadResult indicating success and node statistics
     */
    suspend fun uploadSecret(
        id: String,
        request: UploadRequest,
        signature: String,
        blindedShares: List<ByteArray>,
    ): UploadResult =
        coroutineScope {
            requireInitialized()
            require(blindedShares.size == config.totalNodes) {
                "Must provide ${config.totalNodes} blinded shares, got ${blindedShares.size}"
            }

            // Create upload sharing data for each node
            val sharingRequests =
                blindedShares.mapIndexed { index, blindedShare ->
                    Sharing(
                        shareCommitments = request.shareCommitments,
                        recoveringAddresses = request.recoveringAddresses,
                        shareData = blindedShare.toHexString(),
                    )
                }

            // Upload to all nodes in parallel
            val results =
                nodeClients
                    .mapIndexed { index, client ->
                        async {
                            try {
                                client.sendUpload(id, sharingRequests[index], signature)
                                true
                            } catch (e: Exception) {
                                println("Upload to node $index failed: ${e.message}")
                                false
                            }
                        }
                    }.awaitAll()

            val successCount = results.count { it }
            UploadResult(
                successCount = successCount,
                totalNodes = config.totalNodes,
                successful = successCount >= config.minSuccessfulNodes,
            )
        }

    /**
     * Download and reconstruct a secret from distributed shares.
     *
     * @param id Unique identifier of the secret
     * @param request Download request with recovery address and ephemeral public key
     * @param signature Pre-signed authorization signature
     * @param ephemeralSecretKey Secret key for decrypting shares (32 bytes)
     * @return The reconstructed secret, or null if insufficient shares
     */
    suspend fun downloadSecret(
        id: String,
        request: DownloadRequest,
        signature: String,
        ephemeralSecretKey: ByteArray,
    ): ByteArray? =
        coroutineScope {
            requireInitialized()

            // Download from all nodes in parallel
            val shares =
                nodeClients
                    .mapIndexed { index, client ->
                        async {
                            try {
                                val encryptedShare = client.sendDownload(id, request, signature)
                                println(encryptedShare)

                                // Decrypt share using NaCl box
                                val nonce = encryptedShare.nonce.removePrefix("0x").hexToByteArray()
                                val ciphertext = encryptedShare.encryptedShare.removePrefix("0x").hexToByteArray()
                                val nodePubkey = encryptedShare.publicKey.removePrefix("0x").hexToByteArray()
                                val fullMessage = nonce + ciphertext

                                // Decrypt with ephemeral secret key
                                val decryptedBlindedShare = encryption.decrypt(fullMessage, ephemeralSecretKey, nodePubkey)

                                // Remove blinding
                                ShareBlinding.unblind(decryptedBlindedShare)
                            } catch (e: Exception) {
                                println("Download from node $index failed: ${e.message}")
                                null
                            }
                        }
                    }.awaitAll()
                    .filterNotNull()

            if (shares.size < config.threshold) {
                println("Insufficient shares: got ${shares.size}, need ${config.threshold}")
                return@coroutineScope null
            }

            // Reconstruct secret from shares using Shamir's Secret Sharing
            ShamirSharing.combineByteWiseShamir(shares, config.threshold)
        }

    /**
     * Add a new recovery address to an existing secret.
     *
     * @param id Unique identifier of the secret
     * @param request Add address request
     * @param signature Pre-signed authorization signature
     * @return Number of successful operations
     */
    suspend fun addAddress(
        id: String,
        request: AddAddressRequest,
        signature: String,
    ): Int =
        coroutineScope {
            requireInitialized()

            // Send to all nodes in parallel
            val results =
                nodeClients
                    .mapIndexed { index, client ->
                        async {
                            try {
                                client.sendAddAddress(id, request, signature)
                                true
                            } catch (e: Exception) {
                                println("Add address to node $index failed: ${e.message}")
                                false
                            }
                        }
                    }.awaitAll()

            results.count { it }
        }

    /**
     * Remove a recovery address from an existing secret.
     *
     * @param id Unique identifier of the secret
     * @param request Remove address request
     * @param signature Pre-signed authorization signature
     * @return Number of successful operations
     */
    suspend fun removeAddress(
        id: String,
        request: RemoveAddressRequest,
        signature: String,
    ): Int =
        coroutineScope {
            requireInitialized()

            // Send to all nodes in parallel
            val results =
                nodeClients
                    .mapIndexed { index, client ->
                        async {
                            try {
                                client.sendRemoveAddress(id, request, signature)
                                true
                            } catch (e: Exception) {
                                println("Remove address from node $index failed: ${e.message}")
                                false
                            }
                        }
                    }.awaitAll()

            results.count { it }
        }

    /**
     * Update the list of wallet addresses for a secret.
     *
     * @param id Unique identifier of the secret
     * @param request Update wallets request
     * @param signature Pre-signed authorization signature
     * @return Number of successful operations
     */
    suspend fun updateWallets(
        id: String,
        request: UpdateWalletsRequest,
        signature: String,
    ): Int =
        coroutineScope {
            requireInitialized()

            // Send to all nodes in parallel
            val results =
                nodeClients
                    .mapIndexed { index, client ->
                        async {
                            try {
                                client.sendUpdate(id, request, signature)
                                true
                            } catch (e: Exception) {
                                println("Update wallets on node $index failed: ${e.message}")
                                false
                            }
                        }
                    }.awaitAll()

            results.count { it }
        }

    /**
     * Close all node clients and clean up resources.
     */
    fun close() {
        nodeClients.forEach { it.close() }
    }
}
