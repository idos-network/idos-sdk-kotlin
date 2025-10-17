package org.idos.enclave.mpc

import io.ktor.utils.io.core.toByteArray
import org.idos.crypto.Keccak256Hasher
import org.idos.enclave.Enclave
import org.idos.enclave.EnclaveError
import org.idos.enclave.EnclaveKeyType
import org.idos.enclave.EnclaveSessionConfig
import org.idos.enclave.ExpirationChecker
import org.idos.enclave.ExpirationType
import org.idos.enclave.KeyMetadata
import org.idos.enclave.MetadataStorage
import org.idos.enclave.crypto.Encryption
import org.idos.enclave.crypto.ShamirSharing
import org.idos.enclave.crypto.ShareBlinding
import org.idos.enclave.runCatchingErrorAsync
import org.idos.getCurrentTimeMillis
import org.idos.getSecureRandom
import org.idos.kwil.types.UuidString
import org.idos.signer.Signer
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.minutes

/**
 * MPC Enclave for secure encryption/decryption operations using distributed MPC network.
 *
 * This implementation:
 * - Splits secrets using Shamir's Secret Sharing
 * - Distributes shares across multiple MPC nodes
 * - Uses Signer abstraction for wallet-specific authentication
 * - Supports adding/removing recovery addresses
 * - Handles EIP-712 typed data signing
 * - Lazy initialization - node discovery happens on first use
 *
 * @param encryption Platform encryption instance for ephemeral keys and decryption
 * @param storage Metadata storage for tracking stored keys
 * @param mpcConfig MPC configuration (URL, contract, nodes, threshold, etc.)
 * @param signer Wallet signer implementation (EVM/NEAR/XRPL)
 * @param hasher Keccak256 hasher for computing share commitments
 */
class MpcEnclave(
    private val encryption: Encryption,
    private val storage: MetadataStorage,
    private val mpcConfig: MpcConfig,
    private val signer: Signer,
    private val hasher: Keccak256Hasher,
) : Enclave {
    private val mpcClient =
        MpcClient(
            encryption = encryption,
            config = mpcConfig,
        )

    /**
     * Unlock the MPC enclave by downloading secret from the network and storing it locally.
     *
     * This process:
     * 1. Checks for existing valid key (handles expiration)
     * 2. Downloads secret from MPC network if needed
     * 3. Stores secret in platform secure storage
     * 4. Creates metadata with ONE_SHOT expiration by default
     *
     * Session config is read from storage (set via settings screen in app).
     *
     * @param userId id of the user, used as an id for the secret
     * @throws EnclaveError.MpcNotInitialized if nodes cannot be fetched
     * @throws EnclaveError.NoKey if secret not found in MPC network or no metadata exists
     * @throws EnclaveError.KeyExpired if key has expired
     */
    internal suspend fun unlock(
        userId: UuidString,
        sessionConfig: EnclaveSessionConfig,
    ): Unit =
        runCatchingErrorAsync {
            // First check if we already have a valid key
            try {
                ExpirationChecker.check(storage, encryption, EnclaveKeyType.MPC)
                // Key exists and is valid, no need to download
                return@runCatchingErrorAsync
            } catch (e: EnclaveError.NoKey) {
                // No key stored locally, need to download from MPC
            } catch (e: EnclaveError.KeyExpired) {
                // Key expired, need to re-download
                encryption.deleteKey(EnclaveKeyType.MPC)
            }

            // Download secret from MPC network
            val pubkey = downloadSecret(userId)

            val now = getCurrentTimeMillis()
            // Create new metadata with session config
            val newMeta =
                KeyMetadata(
                    publicKey = pubkey.toHexString(),
                    type = EnclaveKeyType.MPC,
                    expirationType = sessionConfig.expirationType,
                    expiresAt = sessionConfig.expirationMillis?.let { now + it },
                    createdAt = now,
                    lastUsedAt = now,
                )

            storage.store(newMeta, EnclaveKeyType.MPC)
        }

    /**
     * Check if enclave has valid (non-expired) key.
     *
     * @throws EnclaveError.NoKey if no key is stored
     * @throws EnclaveError.KeyExpired if the stored key has expired
     */
    internal suspend fun hasValidKey() {
        ExpirationChecker.check(storage, encryption, EnclaveKeyType.MPC)
    }

    /**
     * Enroll by generating a random password and uploading to the network.
     *
     * This is a convenience method for onboarding new users to MPC.
     * It generates a cryptographically secure random password and uploads it to the MPC network.
     *
     * @param userId User identifier for the secret
     * @throws EnclaveError.MpcNotInitialized if nodes cannot be fetched
     * @throws EnclaveError.MpcUploadFailed if upload fails
     */
    suspend fun enroll(userId: UuidString) {
        val password = generatePassword()
        uploadSecret(userId, password)
    }

    private fun generatePassword(): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#\$%^&*()_+-=[]{}|;:,.<>?"
        val length = 20
        val random = getSecureRandom()
        return buildString(length) {
            repeat(length) {
                append(alphabet[random.nextInt(alphabet.length)])
            }
        }
    }

    /**
     * Upload a secret to the MPC network using Shamir's Secret Sharing.
     *
     * The secret is:
     * 1. Split into n shares with threshold k
     * 2. Blinded with random 32 bytes
     * 3. Committed using Keccak256
     * 4. Distributed to all MPC nodes
     * 5. Stored in local secure storage with session config expiration
     *
     * Session config is read from storage (set via settings screen in app).
     *
     * @param userId User identifier for metadata tracking
     * @param secret The secret password used to derive encryption key
     * @return True if upload met minimum success threshold
     * @throws EnclaveError.MpcNotInitialized if nodes cannot be fetched
     * @throws EnclaveError.SignatureFailed if signing fails
     */
    suspend fun uploadSecret(
        userId: UuidString,
        secret: String,
    ) = runCatchingErrorAsync {
        // Split secret into shares
        val shares =
            ShamirSharing.splitByteWiseShamir(
                secret = secret.toByteArray(),
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
        val signableMessage = request.toSignableMessage(mpcConfig.contractAddress)
        val signature = signer.signMessageAsAuthHeader(signableMessage)

        // Upload to nodes (throws MpcUploadFailed if insufficient nodes)
        mpcClient.uploadSecret(userId, request, signature, blindedShares)

        // Store both key and metadata
        // Delete and create new key
        encryption.deleteKey(EnclaveKeyType.MPC)
        val pubkey = encryption.generateKey(userId, secret, EnclaveKeyType.MPC)

        // use 30min as default
        val sessionConfig = EnclaveSessionConfig(ExpirationType.TIMED, 30.minutes.inWholeMilliseconds)

        val now = getCurrentTimeMillis()
        val meta =
            KeyMetadata(
                publicKey = pubkey.toHexString(),
                type = EnclaveKeyType.MPC,
                expirationType = sessionConfig.expirationType,
                expiresAt = sessionConfig.expirationMillis?.let { now + it },
                createdAt = now,
                lastUsedAt = now,
            )
        storage.store(meta, EnclaveKeyType.MPC)
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
     * @return The pubkey of reconstructed key, or null if insufficient shares
     * @throws EnclaveError.MpcNotInitialized if initialize() not called
     * @throws EnclaveError.SignatureFailed if signing fails
     * @throws EnclaveError.KeyGenerationFailed if ephemeral key generation fails
     */
    suspend fun downloadSecret(userId: UuidString): ByteArray =
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
            val signableMessage = request.toSignableMessage(mpcConfig.contractAddress)
            val signature = signer.signMessageAsAuthHeader(signableMessage)

            // Download secret from nodes
            val secret = mpcClient.downloadSecret(userId, request, signature, ephemeralKeyPair.secretKey)

            // Store secret in secure storage
            encryption.deleteKey(EnclaveKeyType.MPC)
            val pubkey = encryption.generateKey(userId, secret.decodeToString(), EnclaveKeyType.MPC)

            return@runCatchingErrorAsync pubkey
        }

    /**
     * Add a new recovery address to an existing secret.
     *
     * This allows another wallet to recover the secret.
     *
     * @param userId User identifier for the secret
     * @param formattedAddressToAdd Formatted recovery address to add (e.g., "eip712:0x...", "NEAR:...", "XRPL:...")
     * @throws EnclaveError.MpcNotInitialized if initialize() not called
     * @throws EnclaveError.MpcManagementFailed if insufficient nodes succeeded
     * @throws EnclaveError.SignatureFailed if signing fails
     */
    suspend fun addAddress(
        userId: UuidString,
        formattedAddressToAdd: String,
    ): Unit =
        runCatchingErrorAsync {
            // Create add address request
            val request =
                AddAddressRequest(
                    recoveringAddress = signer.getFormattedAddress(),
                    addressToAdd = formattedAddressToAdd,
                    timestamp = getCurrentTimeMillis(),
                )

            // Sign the request
            val signableMessage = request.toSignableMessage(mpcConfig.contractAddress)
            val signature = signer.signMessageAsAuthHeader(signableMessage)

            // Send to nodes (throws MpcManagementFailed if insufficient nodes)
            mpcClient.addAddress(userId, request, signature)
        }

    /**
     * Remove a recovery address from an existing secret.
     *
     * This revokes a wallet's ability to recover the secret.
     *
     * @param userId User identifier for the secret
     * @param formattedAddressToRemove Formatted recovery address to remove
     * @throws EnclaveError.MpcNotInitialized if initialize() not called
     * @throws EnclaveError.MpcManagementFailed if insufficient nodes succeeded
     * @throws EnclaveError.SignatureFailed if signing fails
     */
    suspend fun removeAddress(
        userId: UuidString,
        formattedAddressToRemove: String,
    ): Unit =
        runCatchingErrorAsync {
            // Create remove address request
            val request =
                RemoveAddressRequest(
                    recoveringAddress = signer.getFormattedAddress(),
                    addressToRemove = formattedAddressToRemove,
                    timestamp = getCurrentTimeMillis(),
                )

            // Sign the request
            val signableMessage = request.toSignableMessage(mpcConfig.contractAddress)
            val signature = signer.signMessageAsAuthHeader(signableMessage)

            // Send to nodes (throws MpcManagementFailed if insufficient nodes)
            mpcClient.removeAddress(userId, request, signature)
        }

    /**
     * Update the list of wallet addresses for a secret.
     *
     * @param userId User identifier for the secret
     * @param formattedRecoveryAddresses New list of formatted recovery addresses
     * @throws EnclaveError.MpcNotInitialized if initialize() not called
     * @throws EnclaveError.MpcManagementFailed if insufficient nodes succeeded
     * @throws EnclaveError.SignatureFailed if signing fails
     */
    suspend fun updateWallets(
        userId: UuidString,
        formattedRecoveryAddresses: List<String>,
    ): Unit =
        runCatchingErrorAsync {
            // Create update wallets request
            val request =
                UpdateWalletsRequest(
                    recoveringAddresses = formattedRecoveryAddresses,
                    timestamp = getCurrentTimeMillis(),
                )

            // Sign the request
            val signableMessage = request.toSignableMessage(mpcConfig.contractAddress)
            val signature = signer.signMessageAsAuthHeader(signableMessage)

            // Send to nodes (throws MpcManagementFailed if insufficient nodes)
            mpcClient.updateWallets(userId, request, signature)
        }

    /**
     * Decrypt message using the MPC-stored secret.
     *
     * Uses the secret stored locally in secure storage (downloaded via unlock()).
     *
     * @param message Encrypted message bytes
     * @param senderPublicKey Sender's public key
     * @return Decrypted bytes
     * @throws EnclaveError.NoKey if no key is stored
     * @throws EnclaveError.KeyExpired if the stored key has expired
     * @throws EnclaveError.DecryptionFailed if decryption fails
     */
    @Throws(CancellationException::class, EnclaveError::class)
    override suspend fun decrypt(
        message: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray =
        runCatchingErrorAsync {
            val meta = expirationCheck()
            storage.store(meta.copy(lastUsedAt = getCurrentTimeMillis()), EnclaveKeyType.MPC)
            encryption.decrypt(message, senderPublicKey, EnclaveKeyType.MPC)
        }

    /**
     * Encrypt message using the MPC-stored secret.
     *
     * Uses the secret stored locally in secure storage (downloaded via unlock()).
     *
     * @param message Plain message bytes
     * @param receiverPublicKey Receiver's public key
     * @return Pair of (encrypted message with nonce, sender's public key)
     * @throws EnclaveError.NoKey if no key is stored
     * @throws EnclaveError.KeyExpired if the stored key has expired
     * @throws EnclaveError.EncryptionFailed if encryption fails
     */
    @Throws(CancellationException::class, EnclaveError::class)
    override suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> =
        runCatchingErrorAsync {
            val meta = expirationCheck()
            storage.store(meta.copy(lastUsedAt = getCurrentTimeMillis()), EnclaveKeyType.MPC)
            encryption.encrypt(message, receiverPublicKey, EnclaveKeyType.MPC)
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

    private suspend fun expirationCheck(): KeyMetadata = ExpirationChecker.check(storage, encryption, EnclaveKeyType.MPC)
}
