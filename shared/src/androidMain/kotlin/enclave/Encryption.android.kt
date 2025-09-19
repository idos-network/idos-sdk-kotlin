package org.idos.enclave

// TODO: Implement Android-specific encryption using appropriate crypto libraries
class AndroidEncryption : Encryption() {
    override fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> {
        TODO("Android encryption implementation not yet available")
    }

    override fun decrypt(
        fullMessage: ByteArray,
        keyPair: KeyPair,
        senderPublicKey: ByteArray,
    ): ByteArray {
        TODO("Android decryption implementation not yet available")
    }

    override fun generateKeyPair(): KeyPair {
        TODO("Android key pair generation implementation not yet available")
    }

    override fun keyPairFromSecretKey(secretKey: ByteArray): KeyPair {
        TODO("Android key pair from secret key implementation not yet available")
    }
}

// Platform-specific implementation
actual fun getEncryption(): Encryption = AndroidEncryption()
