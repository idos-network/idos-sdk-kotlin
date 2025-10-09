package org.idos.enclave

import kotlinx.serialization.Serializable
import org.idos.getCurrentTimeMillis
import org.idos.kwil.types.HexString
import org.idos.kwil.types.UuidString
import kotlin.coroutines.cancellation.CancellationException

@Serializable
data class KeyMetadata(
    val userId: UuidString,
    val publicKey: HexString,
    val expiredAt: Long,
    val createdAt: Long = getCurrentTimeMillis(),
    val lastUsedAt: Long = getCurrentTimeMillis(),
)

/**
 * Enclave for secure encryption/decryption operations.
 *
 * **⚠️ Do not use directly - use [EnclaveOrchestrator] instead.**
 *
 * This class should only be accessed through [EnclaveOrchestrator], which provides:
 * - Reactive state management via Flow
 * - Proper lifecycle handling
 * - UI-friendly error states
 * - Automatic expiration checks
 *
 * @see EnclaveOrchestrator for the public API and usage documentation
 * @see EnclaveError for error types and handling
 */
open class Enclave internal constructor(
    private val encryption: Encryption,
    private val storage: MetadataStorage,
) {
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
            encryption.deleteKey()
            val pubkey = encryption.generateKey(userId, password)
            val now = getCurrentTimeMillis()
            val meta = KeyMetadata(userId, pubkey.toHexString(), now + expiration)
            storage.store(meta)
            pubkey
        }

    /**
     * Delete encryption key.
     *
     * @throws EnclaveError.StorageFailed if key deletion fails
     */
    internal open suspend fun deleteKey(): Unit =
        runCatchingErrorAsync {
            encryption.deleteKey()
            storage.delete()
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
    open suspend fun decrypt(
        message: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray =
        runCatchingErrorAsync {
            val meta = expirationCheck()
            storage.store(meta.copy(lastUsedAt = getCurrentTimeMillis()))
            encryption.decrypt(message, senderPublicKey)
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
    open suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> =
        runCatchingErrorAsync {
            val meta = expirationCheck()
            storage.store(meta.copy(lastUsedAt = getCurrentTimeMillis()))
            encryption.encrypt(message, receiverPublicKey)
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
            val meta = storage.get() ?: throw EnclaveError.NoKey()

            if (meta.expiredAt < getCurrentTimeMillis()) {
                encryption.deleteKey()
                storage.delete()
                throw EnclaveError.KeyExpired()
            }

            meta
        }
}
