package org.idos.app.security.local

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.idos.app.security.local.LocalSigner.Companion.mnemonicToKeypair
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Exception thrown when there is an error during key generation.
 */
class KeyGenerationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Exception thrown when there is an error during key storage operations.
 */
class KeyStorageException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * A secure key manager that handles only the generation, storage, and retrieval of cryptographic keys
 * using Android's StrongBox hardware security module when available.
 *
 * This class handles ONLY key operations - no address caching or user data.
 */
class KeyManager(
    private val context: Context,
) {
    // Master key for encrypted storage
    private val masterKey: MasterKey by lazy {
        MasterKey
            .Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(true) // Use StrongBox if available
            .build()
    }

    /**
     * Generates a new secure key and stores it in the encrypted storage.
     * Returns the address derived from the key.
     *
     * @param words The mnemonic phrase
     * @param derivationPath The BIP44 derivation path
     */
    @Throws(KeyGenerationException::class)
    suspend fun generateAndStoreKey(
        words: String,
        derivationPath: String,
    ) = withContext(Dispatchers.IO) {
        try {
            val key = words.mnemonicToKeypair(derivationPath)
            // Store the key securely, delete first just in case
            clearStoredKeys()
            storeKey(key)
            key.fill(0)

            Timber.d("Generated and stored new key with derivation path: $derivationPath")
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate key")
            throw KeyGenerationException("Failed to generate key", e)
        }
    }

    /**
     * Stores the provided key data in an encrypted file.
     */
    @Throws(KeyStorageException::class)
    private fun storeKey(keyData: ByteArray) {
        try {
            Timber.d("Storing key to encrypted storage")
            val keyFile = File(context.filesDir, KEY_FILE_NAME)

            val encryptedFile =
                EncryptedFile
                    .Builder(
                        context,
                        keyFile,
                        masterKey,
                        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
                    ).setKeysetAlias(MASTER_KEY_ALIAS)
                    .build()

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
     * Used by EthSigner for signing operations.
     */
    suspend fun getStoredKey(): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                Timber.d("Reading key from encrypted storage")
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
                        ).setKeysetAlias(MASTER_KEY_ALIAS)
                        .build()

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
     */
    @Throws(KeyStorageException::class)
    fun clearStoredKeys() {
        try {
            val keyFile = File(context.filesDir, KEY_FILE_NAME)
            if (keyFile.exists()) {
                keyFile.delete()
            }
            Timber.d("Cleared stored keys")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear stored keys")
            throw KeyStorageException("Failed to clear stored keys", e)
        }
    }

    /**
     * Checks if a key exists in storage
     */
    suspend fun hasStoredKey(): Boolean =
        withContext(Dispatchers.IO) {
            val keyFile = File(context.filesDir, KEY_FILE_NAME)
            keyFile.exists()
        }

    companion object {
        private const val MASTER_KEY_ALIAS = "idos_secure_key_master"
        private const val KEY_FILE_NAME = "secure_key_data"
    }
}
