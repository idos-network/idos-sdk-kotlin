package org.idos.enclave

// https://github.com/idos-network/idos-sdk-js/blob/main/packages/utils/src/enclave/local.ts
class Enclave(
    var userId: String,
    var password: String,
) {
    private val encryption = getEncryption()
    var encryptionProfile: PrivateEncryptionProfile? = null

    /**
     * Decrypt credentials content
     */
    fun decrypt(
        message: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray {
        val profile = this.key()
        return encryption.decrypt(
            message,
            profile.keyPair,
            senderPublicKey,
        )
    }

    fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> = encryption.encrypt(message, receiverPublicKey)

    private fun key(): PrivateEncryptionProfile {
        encryptionProfile?.let { return it }

        val secretKey = Encryption.keyDerivation(password, userId)
        val kp = encryption.keyPairFromSecretKey(secretKey)

        this.encryptionProfile =
            PrivateEncryptionProfile(
                userId = userId,
                password = password,
                keyPair = kp,
            )

        return requireNotNull(encryptionProfile)
    }
}
