package org.idos.enclave

import org.idos.kwil.types.UuidString
import kotlin.coroutines.cancellation.CancellationException

// https://github.com/idos-network/idos-sdk-js/blob/main/packages/utils/src/encryption/index.ts
abstract class Encryption(
    protected val storage: SecureStorage,
) {
    companion object {
        fun keyDerivation(
            password: String,
            salt: String,
        ): ByteArray = KeyDerivation.deriveKey(password, salt)
    }

    @Throws(CancellationException::class, EnclaveError::class)
    internal abstract suspend fun publicKey(secret: ByteArray): ByteArray

    @Throws(CancellationException::class, EnclaveError::class)
    internal abstract suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray>

    @Throws(CancellationException::class, EnclaveError::class)
    internal abstract suspend fun decrypt(
        fullMessage: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray

    internal suspend fun generateKey(
        userId: UuidString,
        password: String,
    ): ByteArray =
        runCatching {
            val secretKey = keyDerivation(password, userId.value)
            storage.storeKey(secretKey)
            val pubkey = publicKey(secretKey)
            secretKey.fill(0)
            pubkey
        }.getOrElse { error ->
            throw EnclaveError.KeyGenerationFailed(error.message ?: "Unknown error")
        }

    internal suspend fun deleteKey() =
        runCatching {
            storage.deleteKey()
        }.getOrElse { error ->
            throw EnclaveError.StorageFailed(error.message ?: "Failed to delete key")
        }

    internal suspend fun getSecretKey(): ByteArray = storage.retrieveKey() ?: throw EnclaveError.NoKey()
}
