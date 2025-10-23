package org.idos.enclave.crypto

import com.iwebpp.crypto.TweetNaclFast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.idos.enclave.DecryptFailure
import org.idos.enclave.EnclaveError
import org.idos.enclave.EnclaveKeyType
import org.idos.enclave.SecureStorage
import java.security.SecureRandom

class JvmEncryption(
    storage: SecureStorage = JvmSecureStorage(),
) : Encryption(storage) {
    private val random = SecureRandom()

    override suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
        enclaveKeyType: EnclaveKeyType,
    ): Pair<ByteArray, ByteArray> =
        withContext(Dispatchers.IO) {
            // Validate receiver public key
            if (receiverPublicKey.size != TweetNaclFast.Box.publicKeyLength) {
                throw EnclaveError.InvalidPublicKey(
                    "Receiver public key must be ${TweetNaclFast.Box.publicKeyLength} bytes, got ${receiverPublicKey.size}",
                )
            }

            // Validate message is not empty
            if (message.isEmpty()) {
                throw EnclaveError.EncryptionFailed("Cannot encrypt empty message")
            }

            val nonce = ByteArray(TweetNaclFast.Box.nonceLength)
            random.nextBytes(nonce)

            val secret = getSecretKey(enclaveKeyType)
            val keypair = TweetNaclFast.Box.keyPair_fromSecretKey(secret)
            val box = TweetNaclFast.Box(receiverPublicKey, keypair.secretKey)

            try {
                val encrypted =
                    box.box(message, nonce)
                        ?: throw EnclaveError.EncryptionFailed("NaCl box encryption returned null")

                val fullMessage = ByteArray(nonce.size + encrypted.size)
                System.arraycopy(nonce, 0, fullMessage, 0, nonce.size)
                System.arraycopy(encrypted, 0, fullMessage, nonce.size, encrypted.size)

                fullMessage to keypair.publicKey
            } finally {
                secret.fill(0)
            }
        }

    override suspend fun decrypt(
        fullMessage: ByteArray,
        senderPublicKey: ByteArray,
        enclaveKeyType: EnclaveKeyType,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            val secret = getSecretKey(enclaveKeyType)
            try {
                decrypt(fullMessage, secret, senderPublicKey)
            } finally {
                secret.fill(0)
            }
        }

    override suspend fun decrypt(
        fullMessage: ByteArray,
        secret: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            // Validate sender public key
            if (senderPublicKey.size != TweetNaclFast.Box.publicKeyLength) {
                throw EnclaveError.InvalidPublicKey(
                    "Sender public key must be ${TweetNaclFast.Box.publicKeyLength} bytes, got ${senderPublicKey.size}",
                )
            }

            val nonceLen = TweetNaclFast.Box.nonceLength
            val macLen = TweetNaclFast.Box.overheadLength

            // Validate message length
            if (fullMessage.size <= nonceLen + macLen) {
                throw EnclaveError.DecryptionFailed(
                    reason = DecryptFailure.InvalidCiphertext,
                    details = "Message too short: ${fullMessage.size} bytes, minimum ${nonceLen + macLen} bytes",
                )
            }

            val nonce = fullMessage.copyOfRange(0, nonceLen)
            val cipherText = fullMessage.copyOfRange(nonceLen, fullMessage.size)

            val box = TweetNaclFast.Box(senderPublicKey, secret)

            val decrypted =
                box.open(cipherText, nonce)
                    ?: throw EnclaveError.DecryptionFailed(
                        reason = DecryptFailure.WrongPassword,
                        details = "NaCl box decryption returned null",
                    )

            decrypted
        }

    override suspend fun publicKey(secret: ByteArray): ByteArray {
        val keypair = TweetNaclFast.Box.keyPair_fromSecretKey(secret)
        return keypair.publicKey
    }

    override fun generateEphemeralKeyPair(): KeyPair {
        val naclKeyPair = TweetNaclFast.Box.keyPair()
        return KeyPair(
            publicKey = naclKeyPair.publicKey,
            secretKey = naclKeyPair.secretKey,
        )
    }
}

// Simple in-memory storage for JVM with KeyType support
class JvmSecureStorage : SecureStorage {
    private val keys = mutableMapOf<EnclaveKeyType, ByteArray>()

    override suspend fun storeKey(
        key: ByteArray,
        enclaveKeyType: EnclaveKeyType,
    ) {
        keys[enclaveKeyType] = key.copyOf()
    }

    override suspend fun retrieveKey(enclaveKeyType: EnclaveKeyType): ByteArray? = keys[enclaveKeyType]?.copyOf()

    override suspend fun deleteKey(enclaveKeyType: EnclaveKeyType) {
        keys[enclaveKeyType]?.fill(0)
        keys.remove(enclaveKeyType)
    }
}
