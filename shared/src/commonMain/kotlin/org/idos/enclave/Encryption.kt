package org.idos.enclave

import org.idos.kwil.types.UuidString

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

    abstract suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray>

    abstract suspend fun decrypt(
        fullMessage: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray

    suspend fun generateKey(
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

    suspend fun deleteKey() =
        runCatching {
            storage.deleteKey()
        }.getOrElse { error ->
            throw EnclaveError.StorageFailed(error.message ?: "Failed to delete key")
        }

    protected suspend fun getSecretKey(): ByteArray = storage.retrieveKey() ?: throw EnclaveError.NoKey()

    protected abstract suspend fun publicKey(secret: ByteArray): ByteArray
}
