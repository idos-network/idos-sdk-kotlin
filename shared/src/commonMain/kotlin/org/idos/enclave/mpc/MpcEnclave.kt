package org.idos.enclave.mpc

import org.idos.crypto.Keccak256Hasher
import org.idos.enclave.Enclave
import org.idos.enclave.EnclaveError
import org.idos.enclave.EnclaveKeyType
import org.idos.enclave.ExpirationType
import org.idos.enclave.KeyMetadata
import org.idos.enclave.MetadataStorage
import org.idos.enclave.crypto.Encryption
import org.idos.enclave.crypto.ShamirSharing
import org.idos.enclave.crypto.ShareBlinding
import org.idos.enclave.runCatchingErrorAsync
import org.idos.getCurrentTimeMillis
import org.idos.kwil.types.HexString
import org.idos.kwil.types.UuidString
import org.idos.signer.Signer
import kotlin.coroutines.cancellation.CancellationException

/**
 * MPC Enclave for secure encryption/decryption operations using distributed MPC network.
 *
 * This implementation:
 * - Splits secrets using Shamir's Secret Sharing
 * - Distributes shares across multiple MPC nodes
 * - Uses Signer abstraction for wallet-specific authentication
 * - Supports adding/removing recovery addresses
 * - Handles EIP-712 typed data signing
 *
 * @param encryption Platform encryption instance for ephemeral keys and decryption
 * @param storage Metadata storage for tracking stored keys
 * @param partisiaRpcUrl URL of the Partisia blockchain RPC endpoint
 * @param contractAddress Address of the MPC contract (hex string without 0x prefix)
 * @param mpcConfig MPC configuration (nodes, threshold, malicious tolerance)
 * @param signer Wallet signer implementation (EVM/NEAR/XRPL)
 */
class MpcEnclave(
    private val encryption: Encryption,
    private val storage: MetadataStorage,
    partisiaRpcUrl: String,
    private val contractAddress: HexString,
    mpcConfig: MpcConfig,
    private val signer: Signer,
    private val hasher: Keccak256Hasher,
) : Enclave {
    private val mpcClient =
        MpcClient(
            partisiaRpcUrl = partisiaRpcUrl,
            contractAddress = contractAddress,
            encryption = encryption,
            config = mpcConfig,
            hasher = hasher,
        )

    /**
     * Initialize the MPC client by discovering nodes from the blockchain.
     *
     * @throws EnclaveError.MpcNotEnoughNodes if insufficient nodes are available
     */
    suspend fun initialize() {
        mpcClient.initialize()
    }

    /**
     * Upload a secret to the MPC network using Shamir's Secret Sharing.
     *
     * The secret is:
     * 1. Split into n shares with threshold k
     * 2. Blinded with random 32 bytes
     * 3. Committed using Keccak256
     * 4. Distributed to all MPC nodes
     *
     * @param userId User identifier for metadata tracking
     * @param secret The secret bytes to upload (e.g., derived key)
     * @return True if upload met minimum success threshold
     * @throws EnclaveError.MpcNotInitialized if initialize() not called
     * @throws EnclaveError.SignatureFailed if signing fails
     */
    suspend fun uploadSecret(
        userId: UuidString,
        secret: ByteArray,
    ): Boolean =
        runCatchingErrorAsync {
            // Split secret into shares
            val shares =
                ShamirSharing.splitByteWiseShamir(
                    secret = secret,
                    n = mpcClient.config.totalNodes,
                    k = mpcClient.config.threshold,
                )

            // Blind all shares
            val blindedShares = shares.map { ShareBlinding.blind(it) }

            // Compute commitments
            val commitments = blindedShares.map { ShareBlinding.computeCommitment(hasher, it) }

            // Create upload request
            val request =
                UploadRequest(
                    shareCommitments = commitments,
                    recoveringAddresses = listOf(signer.getFormattedAddress()),
                )

            // Sign the request
            val signableMessage = request.toSignableMessage(contractAddress)
            val signature = signer.signMessageAsAuthHeader(signableMessage)

            // Upload to nodes
            val result = mpcClient.uploadSecret(userId, request, signature, blindedShares)

            // Store metadata if successful
            if (result.successful) {
                val now = getCurrentTimeMillis()
                val meta =
                    KeyMetadata(
                        userId = userId,
                        publicKey = secret.toHexString(), // For tracking purposes
                        type = EnclaveKeyType.MPC,
                        expirationType = ExpirationType.SESSION, // MPC keys persist until manually deleted
                        expiresAt = null,
                        createdAt = now,
                        lastUsedAt = now,
                    )
                storage.store(meta, EnclaveKeyType.MPC)
            }

            result.successful
        }

    /**
     * Download and reconstruct a secret from the MPC network.
     *
     * This process:
     * 1. Generates ephemeral keypair for secure download
     * 2. Creates download request with ephemeral public key
     * 3. Downloads encrypted shares from nodes
     * 4. Decrypts shares using ephemeral secret key
     * 5. Removes blinding
     * 6. Reconstructs original secret using Shamir
     *
     * @param userId User identifier for the secret
     * @return The reconstructed secret bytes, or null if insufficient shares
     * @throws EnclaveError.MpcNotInitialized if initialize() not called
     * @throws EnclaveError.SignatureFailed if signing fails
     * @throws EnclaveError.KeyGenerationFailed if ephemeral key generation fails
     */
    suspend fun downloadSecret(userId: UuidString): ByteArray? =
        runCatchingErrorAsync {
            // Generate ephemeral keypair for this download
            val ephemeralKeyPair = encryption.generateEphemeralKeyPair()

            // Create download request
            val request =
                DownloadRequest(
                    recoveringAddress = signer.getFormattedAddress(),
                    timestamp = getCurrentTimeMillis(),
                    publicKey = "0x${ephemeralKeyPair.publicKey.toHexString()}",
                )

            // Sign the request
            val signableMessage = request.toSignableMessage(contractAddress)
            val signature = signer.signMessageAsAuthHeader(signableMessage)

            // Download secret from nodes
            val secret = mpcClient.downloadSecret(userId, request, signature, ephemeralKeyPair.secretKey)

            // Update metadata if successful
            if (secret != null) {
                storage.get(EnclaveKeyType.MPC)?.let { meta ->
                    storage.store(meta.copy(lastUsedAt = getCurrentTimeMillis()), EnclaveKeyType.MPC)
                }
            }

            secret
        }

    /**
     * Add a new recovery address to an existing secret.
     *
     * This allows another wallet to recover the secret.
     *
     * @param userId User identifier for the secret
     * @param formattedAddressToAdd Formatted recovery address to add (e.g., "eip712:0x...", "NEAR:...", "XRPL:...")
     * @return Number of successful node operations
     * @throws EnclaveError.MpcNotInitialized if initialize() not called
     * @throws EnclaveError.SignatureFailed if signing fails
     */
    suspend fun addAddress(
        userId: UuidString,
        formattedAddressToAdd: String,
    ): Int =
        runCatchingErrorAsync {
            // Create add address request
            val request =
                AddAddressRequest(
                    recoveringAddress = signer.getFormattedAddress(),
                    addressToAdd = formattedAddressToAdd,
                    timestamp = getCurrentTimeMillis(),
                )

            // Sign the request
            val signableMessage = request.toSignableMessage(contractAddress)
            val signature = signer.signMessageAsAuthHeader(signableMessage)

            // Send to nodes
            mpcClient.addAddress(userId, request, signature)
        }

    /**
     * Remove a recovery address from an existing secret.
     *
     * This revokes a wallet's ability to recover the secret.
     *
     * @param userId User identifier for the secret
     * @param formattedAddressToRemove Formatted recovery address to remove
     * @return Number of successful node operations
     * @throws EnclaveError.MpcNotInitialized if initialize() not called
     * @throws EnclaveError.SignatureFailed if signing fails
     */
    suspend fun removeAddress(
        userId: UuidString,
        formattedAddressToRemove: String,
    ): Int =
        runCatchingErrorAsync {
            // Create remove address request
            val request =
                RemoveAddressRequest(
                    recoveringAddress = signer.getFormattedAddress(),
                    addressToRemove = formattedAddressToRemove,
                    timestamp = getCurrentTimeMillis(),
                )

            // Sign the request
            val signableMessage = request.toSignableMessage(contractAddress)
            val signature = signer.signMessageAsAuthHeader(signableMessage)

            // Send to nodes
            mpcClient.removeAddress(userId, request, signature)
        }

    /**
     * Update the list of wallet addresses for a secret.
     *
     * @param userId User identifier for the secret
     * @param formattedRecoveryAddresses New list of formatted recovery addresses
     * @return Number of successful node operations
     * @throws EnclaveError.MpcNotInitialized if initialize() not called
     * @throws EnclaveError.SignatureFailed if signing fails
     */
    suspend fun updateWallets(
        userId: UuidString,
        formattedRecoveryAddresses: List<String>,
    ): Int =
        runCatchingErrorAsync {
            // Create update wallets request
            val request =
                UpdateWalletsRequest(
                    recoveringAddresses = formattedRecoveryAddresses,
                    timestamp = getCurrentTimeMillis(),
                )

            // Sign the request
            val signableMessage = request.toSignableMessage(contractAddress)
            val signature = signer.signMessageAsAuthHeader(signableMessage)

            // Send to nodes
            mpcClient.updateWallets(userId, request, signature)
        }

    /**
     * Decrypt message using the MPC-recovered secret.
     *
     * This is a convenience method that:
     * 1. Downloads the secret from MPC network
     * 2. Uses it to decrypt the message
     *
     * Note: For better performance, download the secret once and cache it locally.
     *
     * @param message Encrypted message bytes
     * @param senderPublicKey Sender's public key
     * @return Decrypted bytes
     * @throws EnclaveError.NoKey if secret cannot be downloaded
     * @throws EnclaveError.DecryptionFailed if decryption fails
     */
    @Throws(CancellationException::class, EnclaveError::class)
    override suspend fun decrypt(
        message: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray =
        runCatchingErrorAsync {
            // This is a placeholder implementation
            // In practice, you'd need to store the downloaded secret temporarily
            // and use it for decryption via the encryption instance
            throw EnclaveError.NotImplemented("MpcEnclave.decrypt - use downloadSecret() and LocalEnclave instead")
        }

    /**
     * Encrypt message using the MPC-recovered secret.
     *
     * This is a convenience method that:
     * 1. Downloads the secret from MPC network
     * 2. Uses it to encrypt the message
     *
     * Note: For better performance, download the secret once and cache it locally.
     *
     * @param message Plain message bytes
     * @param receiverPublicKey Receiver's public key
     * @return Pair of (encrypted message with nonce, sender's public key)
     * @throws EnclaveError.NoKey if secret cannot be downloaded
     * @throws EnclaveError.EncryptionFailed if encryption fails
     */
    @Throws(CancellationException::class, EnclaveError::class)
    override suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> =
        runCatchingErrorAsync {
            // This is a placeholder implementation
            // In practice, you'd need to store the downloaded secret temporarily
            // and use it for encryption via the encryption instance
            throw EnclaveError.NotImplemented("MpcEnclave.encrypt - use downloadSecret() and LocalEnclave instead")
        }

    /**
     * Delete the MPC-stored secret.
     *
     * @throws EnclaveError.StorageFailed if deletion fails
     */
    suspend fun deleteKey(): Unit =
        runCatchingErrorAsync {
            storage.delete(EnclaveKeyType.MPC)
        }

    /**
     * Close MPC client and clean up resources.
     */
    fun close() {
        mpcClient.close()
    }
}
