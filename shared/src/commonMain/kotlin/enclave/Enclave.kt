package org.idos.enclave

import org.idos.kwil.rpc.UuidString

data class PrivateEncryptionProfile(
    val userId: String,
    val password: String,
    val keyPair: KeyPair,
)

// https://github.com/idos-network/idos-sdk-js/blob/main/packages/utils/src/enclave/local.ts
class Enclave(
    private val encryption: Encryption,
    var userId: UuidString,
    var password: String,
) {
    var encryptionProfile: PrivateEncryptionProfile? = null

    /**
     * Decrypt credentials content
     */
    suspend fun decrypt(
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

    suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> = encryption.encrypt(message, receiverPublicKey)

    private suspend fun key(): PrivateEncryptionProfile {
        encryptionProfile?.let { return it }

        val secretKey = Encryption.keyDerivation(password, userId.value)
        val kp = encryption.keyPairFromSecretKey(secretKey)

        this.encryptionProfile =
            PrivateEncryptionProfile(
                userId = userId.value,
                password = password,
                keyPair = kp,
            )

        return requireNotNull(encryptionProfile)
    }
}
