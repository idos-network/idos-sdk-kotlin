package org.idos.enclave

import kotlinx.serialization.Serializable
import org.idos.getCurrentTimeMillis
import org.idos.kwil.types.HexString
import org.idos.kwil.types.UuidString

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
    open suspend fun generateKey(
        userId: UuidString,
        password: String,
        expiration: Long,
    ): Result<ByteArray> =
        runCatching {
            encryption.deleteKey()
            val pubkey = encryption.generateKey(userId, password)
            val now = getCurrentTimeMillis()
            val meta = KeyMetadata(userId, HexString(pubkey), now + expiration)
            storage.store(meta)
            pubkey
        }.mapError { EnclaveError.KeyGenerationFailed(it.message ?: "Unknown error") }

    /**
     * Delete encryption key.
     *
     * @return Result with Unit or error
     */
    open suspend fun deleteKey(): Result<Unit> =
        runCatching {
            encryption.deleteKey()
            storage.delete()
        }.mapError { EnclaveError.StorageFailed(it.message ?: "Failed to delete key") }

    /**
     * Decrypt message.
     *
     * @param message Encrypted message bytes
     * @param senderPublicKey Sender's public key
     * @return Result with decrypted bytes or EnclaveError
     */
    open suspend fun decrypt(
        message: ByteArray,
        senderPublicKey: ByteArray,
    ): Result<ByteArray> =
        runCatching {
            val meta = expirationCheck().getOrThrow()
            storage.store(meta.copy(lastUsedAt = getCurrentTimeMillis()))
            encryption.decrypt(message, senderPublicKey)
        }.mapError { error ->
            when (error) {
                is EnclaveError -> error
                else ->
                    EnclaveError.DecryptionFailed(
                        reason = DecryptFailure.WrongPassword,
                        details = error.message,
                    )
            }
        }

    /**
     * Encrypt message.
     *
     * @param message Plain message bytes
     * @param receiverPublicKey Receiver's public key
     * @return Result with (ciphertext, nonce) or EnclaveError
     */
    open suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Result<Pair<ByteArray, ByteArray>> =
        runCatching {
            val meta = expirationCheck().getOrThrow()
            storage.store(meta.copy(lastUsedAt = getCurrentTimeMillis()))
            encryption.encrypt(message, receiverPublicKey)
        }.mapError { error ->
            when (error) {
                is EnclaveError -> error
                else -> EnclaveError.EncryptionFailed(error.message ?: "Unknown error")
            }
        }

    /**
     * Check if enclave has valid (non-expired) key.
     *
     * @return Result with Unit if valid, or EnclaveError
     */
    open suspend fun hasValidKey(): Result<Unit> = expirationCheck().map { }

    private suspend fun expirationCheck(): Result<KeyMetadata> =
        runCatching {
            val meta = storage.get() ?: throw EnclaveError.NoKey()

            if (meta.expiredAt < getCurrentTimeMillis()) {
                encryption.deleteKey()
                storage.delete()
                throw EnclaveError.KeyExpired()
            }

            meta
        }

    private fun <T, R> Result<T>.mapError(transform: (Throwable) -> R): Result<T> where R : Throwable =
        this.recoverCatching { error -> throw transform(error) }
}
