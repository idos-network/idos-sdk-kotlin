package org.idos.enclave

import org.idos.kwil.rpc.UuidString

// https://github.com/idos-network/idos-sdk-js/blob/main/packages/utils/src/encryption/index.ts
abstract class Encryption {
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

    abstract suspend fun generateKey(
        userId: UuidString,
        password: String,
    )

    abstract suspend fun deleteKey()
}
