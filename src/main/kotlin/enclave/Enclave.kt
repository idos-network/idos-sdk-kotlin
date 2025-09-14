package org.idos.enclave

import com.iwebpp.crypto.TweetNaclFast

// https://github.com/idos-network/idos-sdk-js/blob/main/packages/utils/src/enclave/local.ts
class Enclave(
    var userId: String,
    var password: String,
) {
    var encryptionProfile: PrivateEncryptionProfile? = null

    /**
     * Decrypt credentials content
     */
    fun decrypt(
        message: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray {
        val profile = this.key()
        return Encryption.decrypt(
            message,
            profile.keyPair,
            senderPublicKey,
        )
    }

    fun encrypt(
        message: ByteArray,
        senderPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> = Encryption.encrypt(message, senderPublicKey)

    private fun key(): PrivateEncryptionProfile {
        encryptionProfile?.let { return it }

        val secretKey = Encryption.keyDerivation(password, userId)
        val kp = TweetNaclFast.Box.keyPair_fromSecretKey(secretKey)

        this.encryptionProfile =
            PrivateEncryptionProfile(
                userId = userId,
                password = password,
                keyPair = kp,
            )

        return requireNotNull(encryptionProfile)
    }
}
