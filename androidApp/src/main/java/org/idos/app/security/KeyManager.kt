package org.idos.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.idos.app.security.EthSigner.Companion.privateToAddress
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

sealed class Address

data class ConnectedAddress(
    val address: String,
) : Address()

object NoAddress : Address()

object LoadingAddress : Address()

class KeyManager(
    private val context: Context,
) : KoinComponent {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val _address = MutableStateFlow<Address>(LoadingAddress)
    val address = _address.asStateFlow()

    // SharedPreferences keys
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("idos_wallet_prefs", Context.MODE_PRIVATE)
    }
    private val keyAddress = "cached_address"

    init {
        scope.launch {
            try {
                // Try to load from SharedPreferences first for faster UI
                val cachedAddress = prefs.getString(keyAddress, "") ?: ""
                if (cachedAddress.isNotEmpty()) {
                    _address.value = ConnectedAddress(cachedAddress)
                    Timber.d("Loaded cached address: $cachedAddress")
                }

                // Then verify with the actual key storage
                getStoredKey()?.let {
                    val address = ConnectedAddress(it.privateToAddress())
                    it.fill(0)
                    if (address != _address.value) {
                        _address.value = address
                        // Update cache
                        prefs.edit { putString(keyAddress, address.address) }
                        Timber.d("Updated address from storage: $address")
                    }
                } ?: run {
                    // Clear cache if no key is stored
                    if (_address.value !is NoAddress) {
                        _address.value = NoAddress
                        prefs.edit { remove(keyAddress) }
                        Timber.d("Cleared address - no key found in storage")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load stored key")
                // Clear cache on error
                prefs.edit { remove(keyAddress) }
                _address.value = NoAddress
            }
        }
    }

    private val masterKey: MasterKey by lazy {
        MasterKey
            .Builder(context, MASTER_KEY_ALIAS)
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
    suspend fun generateAndStoreKey(
        key: ByteArray,
        address: String,
    ): String =
        withContext(Dispatchers.IO) {
            try {
                // Store the key securely
                storeKey(key)
                // cache it but do not trigger new state yet
                prefs.edit { putString(keyAddress, address) }
                Timber.d("Generated and stored new key with address: $address")
                address
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate key")
                // Clear cache on error
                prefs.edit { remove(keyAddress) }
                throw KeyGenerationException("Failed to generate key", e)
            }
        }

    suspend fun notifyAddress() {
        withContext(Dispatchers.IO) {
            val address = prefs.getString(keyAddress, "") ?: ""
            _address.value = ConnectedAddress(address)
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

            val encryptedFile =
                EncryptedFile
                    .Builder(
                        context,
                        keyFile,
                        masterKey,
                        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
                    ).build()

            encryptedFile.openFileOutput().use { outputStream ->
                outputStream.write(keyData)
                outputStream.flush()
            }
            keyData.fill(0)
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
    suspend fun getStoredKey(): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val keyFile = File(context.filesDir, KEY_FILE_NAME)
                if (!keyFile.exists()) {
                    return@withContext null
                }

                val encryptedFile =
                    EncryptedFile
                        .Builder(
                            context,
                            keyFile,
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
            // Clear the cached address
            prefs.edit { remove(keyAddress) }
            _address.value = NoAddress
            Timber.d("Cleared stored keys and cached address")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear stored keys")
            // Clear cache even if other operations fail
            prefs.edit { remove(keyAddress) }
            throw KeyStorageException("Failed to clear stored keys", e)
        }
    }

    companion object {
        private const val MASTER_KEY_ALIAS = "idos_secure_key_master"
        private const val KEY_FILE_NAME = "secure_key_data"
    }
}

/**
 * Exception thrown when there is an error during key generation.
 *
 * @property message The detail message
 * @property cause The cause of this exception
 */
class KeyGenerationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Exception thrown when there is an error during key storage operations.
 *
 * @property message The detail message
 * @property cause The cause of this exception
 */
class KeyStorageException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
