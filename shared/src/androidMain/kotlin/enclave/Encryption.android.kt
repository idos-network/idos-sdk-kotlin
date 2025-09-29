package org.idos.enclave

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.utils.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.idos.kwil.rpc.UuidString
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom

/**
 * Android implementation of [Encryption] using NaCl with StrongBox support.
 * Uses LazySodium (libsodium) for cryptographic operations and Android's
 * StrongBox hardware security module when available.
 *
 * @param context The Android context used for secure key storage
 */
class AndroidEncryption(
    private val context: Context,
) : Encryption() {
    private val sodium = LazySodiumAndroid(SodiumAndroid())
    private val mutex = Mutex()

    private companion object {
        private const val MASTER_KEY_ALIAS = "idos_enclave_master"
        private const val KEY_FILENAME = "encrypted_key"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey
            .Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(true) // Use StrongBox if available
            .build()
    }

    override suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val nonce =
                        ByteArray(SecretBox.NONCEBYTES).also { bytes ->
                            SecureRandom().nextBytes(bytes)
                        }

                    val key = retrieveSecretKey()
                    checkNotNull(key) { "secret key retrieval failed" }
                    val pubkey = sodium.cryptoScalarMultBase(Key.fromBytes(key)).asBytes

                    // Encrypt the message
                    val ciphertext = ByteArray(message.size + SecretBox.MACBYTES)
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

                    check(success) { "Encryption failed" }

                    // Combine nonce and ciphertext
                    val fullMessage = nonce + ciphertext
                    fullMessage to pubkey
                } catch (e: Exception) {
                    throw IllegalStateException("Encryption failed", e)
                }
            }
        }

    override suspend fun decrypt(
        fullMessage: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    require(fullMessage.size > SecretBox.NONCEBYTES) { "Invalid message format" }

                    val key = retrieveSecretKey()
                    checkNotNull(key) { "No secret key found" }

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

                    check(success) { "Decryption failed" }

                    plaintext
                } catch (e: Exception) {
                    throw IllegalStateException("Decryption failed", e)
                }
            }
        }

    override suspend fun generateKey(
        userId: UuidString,
        password: String,
    ) {
        withContext(Dispatchers.IO) {
            val secretKey = keyDerivation(password, userId.value)
            storeSecretKey(secretKey)
            secretKey.fill(0)
        }
    }

    override suspend fun deleteKey() {
        withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, KEY_FILENAME)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun storeSecretKey(secretKey: ByteArray) =
        withContext(Dispatchers.IO) {
            try {
                // Store the secret key in Android's encrypted file storage
                val file = File(context.filesDir, KEY_FILENAME)

                val encryptedFile =
                    EncryptedFile
                        .Builder(
                            context,
                            file,
                            masterKey,
                            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
                        ).setKeysetAlias(MASTER_KEY_ALIAS)
                        .build()

                encryptedFile.openFileOutput().use { outputStream ->
                    outputStream.write(secretKey)
                    outputStream.flush()
                }
                secretKey.fill(0)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to store secret key", e)
            }
        }

    private suspend fun retrieveSecretKey(): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, KEY_FILENAME)
                if (!file.exists()) throw NoKeyError

                val encryptedFile =
                    EncryptedFile
                        .Builder(
                            context,
                            file,
                            masterKey,
                            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
                        ).setKeysetAlias(MASTER_KEY_ALIAS)
                        .build()

                ByteArrayOutputStream().use { outputStream ->
                    encryptedFile.openFileInput().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    outputStream.toByteArray()
                }
            } catch (e: Exception) {
                throw IllegalStateException("Failed to retrieve secret key", e)
            }
        }
}
