package org.idos.enclave

// TODO: Implement iOS-specific encryption using appropriate crypto libraries
class IosEncryption : Encryption() {
    override fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> {
        TODO("iOS encryption implementation not yet available")
    }

    override fun decrypt(
        fullMessage: ByteArray,
        keyPair: KeyPair,
        senderPublicKey: ByteArray,
    ): ByteArray {
        TODO("iOS decryption implementation not yet available")
    }

    override fun generateKeyPair(): KeyPair {
        TODO("iOS key pair generation implementation not yet available")
    }

    override fun keyPairFromSecretKey(secretKey: ByteArray): KeyPair {
        TODO("iOS key pair from secret key implementation not yet available")
    }
}

// Platform-specific implementation
actual fun getEncryption(): Encryption = IosEncryption()
