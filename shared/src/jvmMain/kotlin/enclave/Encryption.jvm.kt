package org.idos.enclave

import com.iwebpp.crypto.TweetNaclFast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom

class JvmEncryption : Encryption() {
    private val random = SecureRandom()

    override suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> =
        withContext(Dispatchers.IO) {
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

            fullMessage to ephKeyPair.publicKey
        }

    override suspend fun decrypt(
        fullMessage: ByteArray,
        keyPair: KeyPair,
        senderPublicKey: ByteArray,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            val nonceLen = TweetNaclFast.Box.nonceLength

            val nonce = fullMessage.copyOfRange(0, nonceLen)
            val cipherText = fullMessage.copyOfRange(nonceLen, fullMessage.size)

            val box = TweetNaclFast.Box(senderPublicKey, keyPair.secretKey)

            val decrypted =
                box.open(cipherText, nonce)
                    ?: throw IllegalStateException("Couldn't decrypt")

            decrypted
        }

    override suspend fun generateKeyPair(): KeyPair =
        withContext(Dispatchers.IO) {
            val naclKeyPair = TweetNaclFast.Box.keyPair()
            TweetNaclKeyPair(naclKeyPair)
        }

    override suspend fun keyPairFromSecretKey(secretKey: ByteArray): KeyPair =
        withContext(Dispatchers.IO) {
            val naclKeyPair = TweetNaclFast.Box.keyPair_fromSecretKey(secretKey)
            TweetNaclKeyPair(naclKeyPair)
        }
}

// Wrapper class to implement our KeyPair interface
class TweetNaclKeyPair(
    private val naclKeyPair: TweetNaclFast.Box.KeyPair,
) : KeyPair {
    override val publicKey: ByteArray = naclKeyPair.publicKey
    override val secretKey: ByteArray = naclKeyPair.secretKey
}

// Platform-specific implementation
actual fun getEncryption(context: Any?): Encryption = JvmEncryption()
