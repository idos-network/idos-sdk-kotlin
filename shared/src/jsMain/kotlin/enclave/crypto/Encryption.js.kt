package org.idos.enclave.crypto

import org.idos.enclave.DecryptFailure
import org.idos.enclave.EnclaveError
import org.idos.enclave.EnclaveKeyType
import org.idos.enclave.SecureStorage
import org.idos.toByteArray
import org.idos.toUint8Array

/**
 * JavaScript/Browser implementation of [Encryption] using tweetnacl.
 * Uses NaCl box (x25519-xsalsa20-poly1305) for authenticated encryption.
 */
class JsEncryption(
    storage: SecureStorage,
) : Encryption(storage) {

    override suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
        enclaveKeyType: EnclaveKeyType,
    ): Pair<ByteArray, ByteArray> {
        // Validate receiver public key
        if (receiverPublicKey.size != TweetNacl.box.publicKeyLength) {
            throw EnclaveError.InvalidPublicKey(
                "Receiver public key must be ${TweetNacl.box.publicKeyLength} bytes, got ${receiverPublicKey.size}",
            )
        }

        // Validate message is not empty
        if (message.isEmpty()) {
            throw EnclaveError.EncryptionFailed("Cannot encrypt empty message")
        }

        val nonce = TweetNacl.randomBytes(TweetNacl.box.nonceLength)
        val secretKey = getSecretKey(enclaveKeyType)
        val keyPair = TweetNacl.box.keyPairFromSecretKey(secretKey.toUint8Array())

        try {
            val encrypted = TweetNacl.box.encrypt(
                message.toUint8Array(),
                nonce,
                receiverPublicKey.toUint8Array(),
                keyPair.secretKey,
            ) ?: throw EnclaveError.EncryptionFailed("NaCl box encryption returned null")

            val fullMessage = ByteArray(nonce.length + encrypted.length)
            nonce.toByteArray().copyInto(fullMessage, 0)
            encrypted.toByteArray().copyInto(fullMessage, nonce.length)

            return fullMessage to keyPair.publicKey.toByteArray()
        } finally {
            secretKey.fill(0)
        }
    }

    override suspend fun decrypt(
        fullMessage: ByteArray,
        senderPublicKey: ByteArray,
        enclaveKeyType: EnclaveKeyType,
    ): ByteArray {
        val secretKey = getSecretKey(enclaveKeyType)
        try {
            return decrypt(fullMessage, secretKey, senderPublicKey)
        } finally {
            secretKey.fill(0)
        }
    }

    override suspend fun decrypt(
        fullMessage: ByteArray,
        secret: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray {
        // Validate sender public key
        if (senderPublicKey.size != TweetNacl.box.publicKeyLength) {
            throw EnclaveError.InvalidPublicKey(
                "Sender public key must be ${TweetNacl.box.publicKeyLength} bytes, got ${senderPublicKey.size}",
            )
        }

        val nonceLen = TweetNacl.box.nonceLength
        val macLen = TweetNacl.box.overheadLength

        // Validate message length
        if (fullMessage.size <= nonceLen + macLen) {
            throw EnclaveError.DecryptionFailed(
                reason = DecryptFailure.InvalidCiphertext,
                details = "Message too short: ${fullMessage.size} bytes, minimum ${nonceLen + macLen} bytes",
            )
        }

        val nonce = fullMessage.copyOfRange(0, nonceLen)
        val ciphertext = fullMessage.copyOfRange(nonceLen, fullMessage.size)

        val decrypted = TweetNacl.box.open(
            ciphertext.toUint8Array(),
            nonce.toUint8Array(),
            senderPublicKey.toUint8Array(),
            secret.toUint8Array(),
        ) ?: throw EnclaveError.DecryptionFailed(
            reason = DecryptFailure.WrongPassword,
            details = "NaCl box decryption returned null",
        )

        return decrypted.toByteArray()
    }

    override fun generateEphemeralKeyPair(): KeyPair {
        val naclKeyPair = TweetNacl.box.keyPair()
        return KeyPair(
            publicKey = naclKeyPair.publicKey.toByteArray(),
            secretKey = naclKeyPair.secretKey.toByteArray(),
        )
    }

    override suspend fun publicKey(secret: ByteArray): ByteArray {
        val keyPair = TweetNacl.box.keyPairFromSecretKey(secret.toUint8Array())
        return keyPair.publicKey.toByteArray()
    }
}
