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
 * All operations return Result<T> for iOS compatibility.
 *
 * @see https://github.com/idos-network/idos-sdk-js/blob/main/packages/utils/src/enclave/local.ts
 */
open class Enclave(
    private val encryption: Encryption,
    private val storage: MetadataStorage,
) {
    /**
     * Generate new encryption key.
     *
     * @param userId User identifier for key derivation
     * @param password Password for key derivation
     * @param expiration Key expiration time in milliseconds
     * @return Result with public key bytes or error
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
            val meta = KeyMetadata(userId, HexString(pubkey), now + expiration)
            storage.store(meta)
            pubkey
        }

    /**
     * Delete encryption key.
     *
     * @return Result with Unit or error
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
     * @return Result with decrypted bytes or EnclaveError
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
     * @return Result with (ciphertext, nonce) or EnclaveError
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
     * @return Result with Unit if valid, or EnclaveError
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
