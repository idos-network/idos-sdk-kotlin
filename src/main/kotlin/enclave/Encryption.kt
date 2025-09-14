package org.idos.enclave

import com.iwebpp.crypto.TweetNaclFast
import java.security.SecureRandom

// https://github.com/idos-network/idos-sdk-js/blob/main/packages/utils/src/encryption/index.ts
object Encryption {
    private val random = SecureRandom()

    fun keyDerivation(
        password: String,
        salt: String,
    ): ByteArray = idOSKeyDerivation.deriveKey(password, salt)

    fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> {
        val nonce = ByteArray(TweetNaclFast.Box.nonceLength)
        random.nextBytes(nonce)

        val ephKeyPair = TweetNaclFast.Box.keyPair()

        val box = TweetNaclFast.Box(receiverPublicKey, ephKeyPair.secretKey)

        val encrypted =
            box.box(message, nonce)
                ?: throw IllegalStateException("Couldn't encrypt")

        val fullMessage = ByteArray(nonce.size + encrypted.size)
        System.arraycopy(nonce, 0, fullMessage, 0, nonce.size)
        System.arraycopy(encrypted, 0, fullMessage, nonce.size, encrypted.size)

        return fullMessage to ephKeyPair.publicKey
    }

    fun decrypt(
        fullMessage: ByteArray,
        keyPair: TweetNaclFast.Box.KeyPair,
        senderPublicKey: ByteArray,
    ): ByteArray {
        val nonceLen = TweetNaclFast.Box.nonceLength

        val nonce = fullMessage.copyOfRange(0, nonceLen)
        val cipherText = fullMessage.copyOfRange(nonceLen, fullMessage.size)

        val box = TweetNaclFast.Box(senderPublicKey, keyPair.secretKey)

        val decrypted =
            box.open(cipherText, nonce)
                ?: throw IllegalStateException("Couldn't decrypt")

        return decrypted
    }
}
