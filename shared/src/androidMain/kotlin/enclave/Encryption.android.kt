package org.idos.enclave

import android.content.Context
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.utils.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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

    // Key aliases for Android Keystore
    private companion object {
        private const val KEY_ALIAS = "idos_nacl_keypair"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val KEY_SIZE = 256
        private const val KEYSTORE_ALIAS = "idos_nacl_keystore"
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    }

    private val masterKey: MasterKey by lazy {
        MasterKey
            .Builder(context)
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

                    // Generate ephemeral key pair for this encryption
                    val ephKeyPair = sodium.cryptoBoxKeypair()

                    // Encrypt the message
                    val ciphertext = ByteArray(message.size + SecretBox.MACBYTES)
                    val success =
                        sodium.cryptoBoxEasy(
                            ciphertext,
                            message,
                            message.size.toLong(),
                            nonce,
                            receiverPublicKey,
                            ephKeyPair.secretKey.asBytes,
                        )

                    if (!success) {
                        throw IllegalStateException("Encryption failed")
                    }

                    // Combine nonce and ciphertext
                    val fullMessage = nonce + ciphertext
                    fullMessage to ephKeyPair.publicKey.asBytes
                } catch (e: Exception) {
                    throw IllegalStateException("Encryption failed", e)
                }
            }
        }

    override suspend fun decrypt(
        fullMessage: ByteArray,
        keyPair: KeyPair,
        senderPublicKey: ByteArray,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    if (fullMessage.size <= SecretBox.NONCEBYTES) {
                        throw IllegalArgumentException("Invalid message format")
                    }

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
                            keyPair.secretKey,
                        )

                    if (!success) {
                        throw IllegalStateException("Decryption failed")
                    }

                    plaintext
                } catch (e: Exception) {
                    throw IllegalStateException("Decryption failed", e)
                }
            }
        }

    override suspend fun generateKeyPair(): KeyPair {
        val keyPair = sodium.cryptoBoxKeypair()

        // Store the secret key securely
        storeSecretKey(keyPair.secretKey.asBytes)

        return object : KeyPair {
            override val publicKey: ByteArray = keyPair.publicKey.asBytes
            override val secretKey: ByteArray = keyPair.secretKey.asBytes
        }
    }

    override suspend fun keyPairFromSecretKey(secretKey: ByteArray): KeyPair {
        // Generate a new key pair and return it with the provided secret key
        val pubkey = sodium.cryptoScalarMultBase(Key.fromBytes(secretKey))
        return object : KeyPair {
            override val publicKey: ByteArray = pubkey.asBytes
            override val secretKey: ByteArray = secretKey
        }
    }

    private suspend fun storeSecretKey(secretKey: ByteArray) {
        try {
            // Store the secret key in Android's encrypted file storage
            val file = File("${android.os.Environment.getDataDirectory()}/encrypted_nacl_key")

            val encryptedFile =
                EncryptedFile
                    .Builder(
                        context,
                        file,
                        masterKey,
                        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
                    ).build()

            encryptedFile.openFileOutput().use { outputStream ->
                outputStream.write(secretKey)
                outputStream.flush()
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to store secret key", e)
        }
    }

    private suspend fun retrieveSecretKey(): ByteArray? {
        return try {
            val file = File("${android.os.Environment.getDataDirectory()}/encrypted_nacl_key")
            if (!file.exists()) return null

            val encryptedFile =
                EncryptedFile
                    .Builder(
                        context,
                        file,
                        masterKey,
                        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
                    ).build()

            ByteArrayOutputStream().use { outputStream ->
                encryptedFile.openFileInput().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
                outputStream.toByteArray()
            }
        } catch (e: Exception) {
            null
        }
    }
}

// Platform-specific implementation
actual fun getEncryption(context: Any?): Encryption {
    require(context is Context) { "Android Encryption requires an Android Context" }
    return AndroidEncryption(context)
}
