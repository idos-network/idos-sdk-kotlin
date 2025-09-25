package org.idos.app.security

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.bip32.toKey
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toSeed
import org.kethereum.crypto.toAddress
import org.kethereum.crypto.toECKeyPair
import org.kethereum.model.PrivateKey
import org.koin.core.component.KoinComponent
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore

/**
 * A secure key manager that handles the generation, storage, and retrieval of cryptographic keys
 * using Android's StrongBox hardware security module when available.
 *
 * This class provides a simple interface for generating secure keys, storing them in an
 * encrypted file, and retrieving them when needed. All operations are performed asynchronously
 * using coroutines.
 *
 * @property context The application context used for file operations and key storage.
 */

class KeyManager(private val context: Context) : KoinComponent {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val _address = MutableStateFlow<String>("")
    val address = _address.asStateFlow()

    init {
        scope.launch {
            try {
                getStoredKey()?.let {
                    val private = PrivateKey(it)
                    val address = private.toECKeyPair().publicKey.toAddress().hex
                    _address.value = address
                    Timber.d("Loaded stored address: $address")
                } ?: run {
                    Timber.d("No stored key found")
                    _address.value = "" // Clear the address when no key is found
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load stored key")
                _address.value = "" // Clear the address on error
            }
        }
    }

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(true) // Use StrongBox if available
            .build()
    }

    /**
     * Generates a new secure key and stores it in the encrypted storage.
     *
     * @return The generated key as a byte array.
     * @throws KeyGenerationException If the key generation or storage fails.
     */
    @Throws(KeyGenerationException::class)
    suspend fun generateAndStoreKey(words: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val mnemonic = MnemonicWords(words)
                val seed = mnemonic.toSeed("")
                val key = seed.toKey("m/44'/60'/0'/0/47")
                // Store the key securely
                storeKey(key.keyPair.privateKey.key.toByteArray())
                val address = key.keyPair.toAddress().hex
                _address.value = address // This will emit the new address to all collectors
                Timber.d("Generated and stored new address: $address")
                address // Return the generated address
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate key")
                throw KeyGenerationException("Failed to generate key", e)
            }
        }
    }

    /**
     * Stores the provided key data in an encrypted file.
     *
     * @param keyData The key data to store.
     * @throws KeyStorageException If the key storage fails.
     */
    @Throws(KeyStorageException::class)
    private fun storeKey(keyData: ByteArray) {
        try {
            val keyFile = File(context.filesDir, KEY_FILE_NAME)
            val masterKey = MasterKey.Builder(context, "${MASTER_KEY_ALIAS}_master")
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setRequestStrongBoxBacked(true)
                .build()

            val encryptedFile = EncryptedFile.Builder(
                context,
                keyFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            encryptedFile.openFileOutput().use { outputStream ->
                outputStream.write(keyData)
                outputStream.flush()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to store key")
            throw KeyStorageException("Failed to store key", e)
        }
    }

    /**
     * Retrieves the stored key from the encrypted storage.
     *
     * @return The stored key as a byte array, or null if no key is stored.
     */
    suspend fun getStoredKey(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val keyFile = File(context.filesDir, KEY_FILE_NAME)
            if (!keyFile.exists()) {
                return@withContext null
            }

            val masterKey = MasterKey.Builder(context, "${MASTER_KEY_ALIAS}_master")
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setRequestStrongBoxBacked(true)
                .build()

            val encryptedFile = EncryptedFile.Builder(
                context,
                keyFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            ByteArrayOutputStream().use { outputStream ->
                encryptedFile.openFileInput().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
                outputStream.toByteArray()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read key")
            null
        }
    }

    /**
     * Clears the stored key from the encrypted storage.
     *
     * @throws KeyStorageException If the key deletion fails.
     */
    @Throws(KeyStorageException::class)
    fun clearStoredKeys() {
        try {
            val keyFile = File(context.filesDir, KEY_FILE_NAME)
            if (keyFile.exists()) {
                keyFile.delete()
            }
            _address.value = "" // Clear the address
            Timber.d("Cleared stored keys and address")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear stored keys")
            throw KeyStorageException("Failed to clear stored keys", e)
        }
    }

    companion object {
        private const val MASTER_KEY_ALIAS = "idos_secure_key"
        private const val KEY_FILE_NAME = "secure_key_data"
    }
}

/**
 * Exception thrown when there is an error during key generation.
 *
 * @property message The detail message
 * @property cause The cause of this exception
 */
class KeyGenerationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when there is an error during key storage operations.
 *
 * @property message The detail message
 * @property cause The cause of this exception
 */
class KeyStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)
