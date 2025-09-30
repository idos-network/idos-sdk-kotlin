package org.idos.enclave

import org.idos.kwil.rpc.UuidString

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
    ): ByteArray {
        val secretKey = keyDerivation(password, userId.value)
        storage.storeKey(secretKey)
        val pubkey = publicKey(secretKey)
        secretKey.fill(0)
        return pubkey
    }

    suspend fun deleteKey() {
        storage.deleteKey()
    }

    protected suspend fun getSecretKey(): ByteArray = storage.retrieveKey() ?: throw NoKeyError

    protected abstract suspend fun publicKey(secret: ByteArray): ByteArray
}
