package org.idos.enclave

import com.iwebpp.crypto.TweetNaclFast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom

class JvmEncryption(
    storage: SecureStorage = JvmSecureStorage(),
) : Encryption(storage) {
    private val random = SecureRandom()

    override suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> =
        withContext(Dispatchers.IO) {
            val nonce = ByteArray(TweetNaclFast.Box.nonceLength)
            random.nextBytes(nonce)

            val secret = getSecretKey()
            val keypair = TweetNaclFast.Box.keyPair_fromSecretKey(secret)
            val box = TweetNaclFast.Box(receiverPublicKey, keypair.secretKey)

            val encrypted =
                box.box(message, nonce)
                    ?: throw IllegalStateException("Couldn't encrypt")

            val fullMessage = ByteArray(nonce.size + encrypted.size)
            System.arraycopy(nonce, 0, fullMessage, 0, nonce.size)
            System.arraycopy(encrypted, 0, fullMessage, nonce.size, encrypted.size)

            secret.fill(0)
            fullMessage to keypair.publicKey
        }

    override suspend fun decrypt(
        fullMessage: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            val nonceLen = TweetNaclFast.Box.nonceLength

            val nonce = fullMessage.copyOfRange(0, nonceLen)
            val cipherText = fullMessage.copyOfRange(nonceLen, fullMessage.size)

            val secret = getSecretKey()
            val box = TweetNaclFast.Box(senderPublicKey, secret)

            val decrypted =
                box.open(cipherText, nonce)
                    ?: throw IllegalStateException("Couldn't decrypt")

            secret.fill(0)
            decrypted
        }

    override suspend fun publicKey(secret: ByteArray): ByteArray {
        val keypair = TweetNaclFast.Box.keyPair_fromSecretKey(secret)
        return keypair.publicKey
    }
}

// Simple in-memory storage for JVM
class JvmSecureStorage : SecureStorage {
    private var key: ByteArray? = null

    override suspend fun storeKey(key: ByteArray) {
        this.key = key.copyOf()
    }

    override suspend fun retrieveKey(): ByteArray? = key?.copyOf()

    override suspend fun deleteKey() {
        key?.fill(0)
        key = null
    }
}
