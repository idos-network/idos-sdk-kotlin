package org.idos.enclave

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
        keyPair: KeyPair,
        senderPublicKey: ByteArray,
    ): ByteArray

    abstract suspend fun generateKeyPair(): KeyPair

    abstract suspend fun keyPairFromSecretKey(secretKey: ByteArray): KeyPair
}

// Common interface for KeyPair to abstract away platform-specific implementations
interface KeyPair {
    val publicKey: ByteArray
    val secretKey: ByteArray
}

// Get platform-specific encryption implementation
expect fun getEncryption(context: Any? = null): Encryption
