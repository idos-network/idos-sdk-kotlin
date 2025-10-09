package org.idos.enclave

import android.content.Context
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.Box
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.utils.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
    private val mutex = Mutex()

    override suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                // todo add public key & message validation

                val nonce =
                    ByteArray(Box.NONCEBYTES).also { bytes ->
                        SecureRandom().nextBytes(bytes)
                    }

                val key = getSecretKey()
                val pubkey = publicKey(key)

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
                key.fill(0)

                if (!success) {
                    throw EnclaveError.EncryptionFailed("Libsodium crypto_box_easy failed")
                }

                // Combine nonce and ciphertext
                val fullMessage = nonce + ciphertext
                fullMessage to pubkey
            }
        }

    override suspend fun decrypt(
        fullMessage: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                // todo add public key validation

                if (fullMessage.size <= SecretBox.NONCEBYTES) {
                    throw EnclaveError.DecryptionFailed(
                        reason = DecryptFailure.InvalidCiphertext,
                        details = "Message too short",
                    )
                }

                val key = getSecretKey()

                val nonce = fullMessage.copyOfRange(0, SecretBox.NONCEBYTES)
                val ciphertext = fullMessage.copyOfRange(SecretBox.NONCEBYTES, fullMessage.size)

                val plaintext = ByteArray(ciphertext.size - SecretBox.MACBYTES)
                val success =
                    sodium.cryptoBoxOpenEasy(
                        plaintext,
                        ciphertext,
                        ciphertext.size.toLong(),
                        nonce,
                        senderPublicKey,
                        key,
                    )
                key.fill(0)

                if (!success) {
                    throw EnclaveError.DecryptionFailed(
                        reason = DecryptFailure.WrongPassword,
                        details = "Libsodium crypto_box_open_easy failed",
                    )
                }

                plaintext
            }
        }

    override suspend fun publicKey(secret: ByteArray): ByteArray {
        val pubkey = sodium.cryptoScalarMultBase(Key.fromBytes(secret)).asBytes
        return pubkey
    }
}
