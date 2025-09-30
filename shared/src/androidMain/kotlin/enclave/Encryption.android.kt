package org.idos.enclave

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.Box
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.utils.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
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
                try {
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

                    check(success) { "Decryption failed" }

                    plaintext
                } catch (e: Exception) {
                    throw IllegalStateException("Decryption failed", e)
                }
            }
        }

    override suspend fun publicKey(secret: ByteArray): ByteArray {
        val pubkey = sodium.cryptoScalarMultBase(Key.fromBytes(secret)).asBytes
        return pubkey
    }
}

/**
 * Android secure storage using EncryptedFile with StrongBox support.
 */
class AndroidSecureStorage(
    private val context: Context,
) : SecureStorage {
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

    override suspend fun storeKey(key: ByteArray) =
        withContext(Dispatchers.IO) {
            try {
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
                    outputStream.write(key)
                    outputStream.flush()
                }
            } catch (e: Exception) {
                throw IllegalStateException("Failed to store secret key", e)
            }
        }

    override suspend fun retrieveKey(): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, KEY_FILENAME)
                if (!file.exists()) return@withContext null

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

    override suspend fun deleteKey() =
        withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, KEY_FILENAME)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Ignore deletion errors
            }
        }
}
