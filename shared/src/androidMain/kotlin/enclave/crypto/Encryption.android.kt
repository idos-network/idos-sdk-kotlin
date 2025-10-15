package org.idos.enclave.crypto

import android.content.Context
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.Box
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.utils.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.idos.enclave.AndroidSecureStorage
import org.idos.enclave.DecryptFailure
import org.idos.enclave.EnclaveError
import org.idos.enclave.EnclaveKeyType
import org.idos.enclave.SecureStorage
import java.security.SecureRandom

/**
 * Android implementation of [Encryption] using NaCl with StrongBox support.
 * Uses LazySodium (libsodium) for cryptographic operations.
 *
 * @param storage The secure storage implementation for key persistence
 */
class AndroidEncryption(
    storage: SecureStorage,
) : Encryption(storage) {
    constructor(context: Context) : this(AndroidSecureStorage(context))

    private val sodium by lazy { LazySodiumAndroid(SodiumAndroid()) }

    override suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
        enclaveKeyType: EnclaveKeyType,
    ): Pair<ByteArray, ByteArray> =
        withContext(Dispatchers.IO) {
            // Validate receiver public key
            if (receiverPublicKey.size != Box.PUBLICKEYBYTES) {
                throw EnclaveError.InvalidPublicKey(
                    "Receiver public key must be ${Box.PUBLICKEYBYTES} bytes, got ${receiverPublicKey.size}",
                )
            }

            // Validate message is not empty
            if (message.isEmpty()) {
                throw EnclaveError.EncryptionFailed("Cannot encrypt empty message")
            }

            val nonce =
                ByteArray(Box.NONCEBYTES).also { bytes ->
                    SecureRandom().nextBytes(bytes)
                }

            val key = getSecretKey(enclaveKeyType)
            val pubkey = publicKey(key)

            try {
                // Encrypt the message
                val ciphertext = ByteArray(message.size + Box.MACBYTES)
                val success =
                    sodium.cryptoBoxEasy(
                        ciphertext,
                        message,
                        message.size.toLong(),
                        nonce,
                        receiverPublicKey,
                        key,
                    )

                if (!success) {
                    throw EnclaveError.EncryptionFailed("Libsodium crypto_box_easy failed")
                }

                // Combine nonce and ciphertext
                val fullMessage = nonce + ciphertext
                fullMessage to pubkey
            } finally {
                key.fill(0)
            }
        }

    override suspend fun decrypt(
        fullMessage: ByteArray,
        senderPublicKey: ByteArray,
        enclaveKeyType: EnclaveKeyType,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            val key = getSecretKey(enclaveKeyType)
            try {
                decrypt(fullMessage, key, senderPublicKey)
            } finally {
                key.fill(0)
            }
        }

    override suspend fun decrypt(
        fullMessage: ByteArray,
        secret: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            // Validate sender public key
            if (senderPublicKey.size != Box.PUBLICKEYBYTES) {
                throw EnclaveError.InvalidPublicKey(
                    "Sender public key must be ${Box.PUBLICKEYBYTES} bytes, got ${senderPublicKey.size}",
                )
            }

            // Validate message length
            if (fullMessage.size <= Box.NONCEBYTES + Box.MACBYTES) {
                throw EnclaveError.DecryptionFailed(
                    reason = DecryptFailure.InvalidCiphertext,
                    details = "Message too short: ${fullMessage.size} bytes, minimum ${Box.NONCEBYTES + Box.MACBYTES} bytes",
                )
            }

            val nonce = fullMessage.copyOfRange(0, Box.NONCEBYTES)
            val ciphertext = fullMessage.copyOfRange(Box.NONCEBYTES, fullMessage.size)

            val plaintext = ByteArray(ciphertext.size - Box.MACBYTES)
            val success =
                sodium.cryptoBoxOpenEasy(
                    plaintext,
                    ciphertext,
                    ciphertext.size.toLong(),
                    nonce,
                    senderPublicKey,
                    secret,
                )

            if (!success) {
                throw EnclaveError.DecryptionFailed(
                    reason = DecryptFailure.WrongPassword,
                    details = "Libsodium crypto_box_open_easy failed",
                )
            }

            plaintext
        }

    override fun generateEphemeralKeyPair(): KeyPair {
        try {
            val keyPair = sodium.cryptoBoxKeypair()
            return KeyPair(keyPair.publicKey.asBytes, keyPair.secretKey.asBytes)
        } catch (e: Exception) {
            throw EnclaveError.KeyGenerationFailed(e.message ?: "unknown error")
        }
    }

    override suspend fun publicKey(secret: ByteArray): ByteArray {
        val pubkey = sodium.cryptoScalarMultBase(Key.fromBytes(secret)).asBytes
        return pubkey
    }
}
