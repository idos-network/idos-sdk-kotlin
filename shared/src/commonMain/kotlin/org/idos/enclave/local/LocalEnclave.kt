package org.idos.enclave.local

import org.idos.enclave.Enclave
import org.idos.enclave.EnclaveError
import org.idos.enclave.EnclaveKeyType
import org.idos.enclave.ExpirationType
import org.idos.enclave.KeyMetadata
import org.idos.enclave.MetadataStorage
import org.idos.enclave.crypto.Encryption
import org.idos.enclave.runCatchingErrorAsync
import org.idos.getCurrentTimeMillis
import org.idos.kwil.types.UuidString
import kotlin.coroutines.cancellation.CancellationException

/**
 * Local Enclave for secure encryption/decryption operations using platform secure storage.
 *
 * **⚠️ Do not use directly - use [org.idos.enclave.EnclaveOrchestrator] instead.**
 *
 * This class should only be accessed through [org.idos.enclave.EnclaveOrchestrator], which provides:
 * - Reactive state management via Flow
 * - Proper lifecycle handling
 * - UI-friendly error states
 * - Automatic expiration checks
 *
 * @see org.idos.enclave.EnclaveOrchestrator for the public API and usage documentation
 * @see EnclaveError for error types and handling
 */
open class LocalEnclave internal constructor(
    private val encryption: Encryption,
    private val storage: MetadataStorage,
) : Enclave {
    /**
     * Generate new encryption key.
     *
     * @param userId User identifier for key derivation
     * @param password Password for key derivation
     * @param expiration Key expiration time in milliseconds
     * @return Public key bytes
     * @throws EnclaveError.KeyGenerationFailed if key generation fails
     */
    internal open suspend fun generateKey(
        userId: UuidString,
        password: String,
        expiration: Long,
    ): ByteArray =
        runCatchingErrorAsync {
            encryption.deleteKey(EnclaveKeyType.LOCAL)
            val pubkey = encryption.generateKey(userId, password)
            val now = getCurrentTimeMillis()
            val meta =
                KeyMetadata(
                    userId = userId,
                    publicKey = pubkey.toHexString(),
                    type = EnclaveKeyType.LOCAL,
                    expirationType = ExpirationType.TIMED,
                    expiresAt = now + expiration,
                )
            storage.store(meta, EnclaveKeyType.LOCAL)
            pubkey
        }

    /**
     * Delete encryption key.
     *
     * @throws EnclaveError.StorageFailed if key deletion fails
     */
    internal open suspend fun deleteKey(): Unit =
        runCatchingErrorAsync {
            encryption.deleteKey(EnclaveKeyType.LOCAL)
            storage.delete(EnclaveKeyType.LOCAL)
        }

    /**
     * Decrypt message.
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
            storage.store(meta.copy(lastUsedAt = getCurrentTimeMillis()), EnclaveKeyType.LOCAL)
            encryption.decrypt(message, senderPublicKey, EnclaveKeyType.LOCAL)
        }

    /**
     * Encrypt message.
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
            storage.store(meta.copy(lastUsedAt = getCurrentTimeMillis()), EnclaveKeyType.LOCAL)
            encryption.encrypt(message, receiverPublicKey, EnclaveKeyType.LOCAL)
        }

    /**
     * Check if enclave has valid (non-expired) key.
     *
     * @throws EnclaveError.NoKey if no key is stored
     * @throws EnclaveError.KeyExpired if the stored key has expired
     */
    internal open suspend fun hasValidKey() {
        expirationCheck()
    }

    private suspend fun expirationCheck(): KeyMetadata =
        runCatchingErrorAsync {
            val meta = storage.get(EnclaveKeyType.LOCAL) ?: throw EnclaveError.NoKey()
            val now = getCurrentTimeMillis()

            when (meta.expirationType) {
                ExpirationType.TIMED -> {
                    if (meta.expiresAt != null && now > meta.expiresAt) {
                        encryption.deleteKey(EnclaveKeyType.LOCAL)
                        storage.delete(EnclaveKeyType.LOCAL)
                        throw EnclaveError.KeyExpired()
                    }
                }
                ExpirationType.ONE_SHOT -> {
                    if (meta.lastUsedAt != meta.createdAt) {
                        // Already used once, auto-lock
                        encryption.deleteKey(EnclaveKeyType.LOCAL)
                        storage.delete(EnclaveKeyType.LOCAL)
                        throw EnclaveError.KeyExpired()
                    }
                }
                ExpirationType.SESSION -> {
                    // No auto-expiration, manual lock() required
                }
            }

            meta
        }
}
