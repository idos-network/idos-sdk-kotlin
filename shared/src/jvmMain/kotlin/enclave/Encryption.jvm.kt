package org.idos.enclave

import com.iwebpp.crypto.TweetNaclFast
import java.security.SecureRandom

class JvmEncryption : Encryption() {
    private val random = SecureRandom()

    override fun encrypt(
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

    override fun decrypt(
        fullMessage: ByteArray,
        keyPair: KeyPair,
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

    override fun generateKeyPair(): KeyPair {
        val naclKeyPair = TweetNaclFast.Box.keyPair()
        return TweetNaclKeyPair(naclKeyPair)
    }

    override fun keyPairFromSecretKey(secretKey: ByteArray): KeyPair {
        val naclKeyPair = TweetNaclFast.Box.keyPair_fromSecretKey(secretKey)
        return TweetNaclKeyPair(naclKeyPair)
    }
}

// Wrapper class to implement our KeyPair interface
class TweetNaclKeyPair(private val naclKeyPair: TweetNaclFast.Box.KeyPair) : KeyPair {
    override val publicKey: ByteArray = naclKeyPair.publicKey
    override val secretKey: ByteArray = naclKeyPair.secretKey
}

// Platform-specific implementation
actual fun getEncryption(): Encryption = JvmEncryption()
