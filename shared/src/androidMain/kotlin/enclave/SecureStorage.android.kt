package org.idos.enclave

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Android secure storage using EncryptedFile with StrongBox support.
 * Matches iOS KeychainSecureStorage for consistent security across platforms.
 */
class AndroidSecureStorage(
    private val context: Context,
) : SecureStorage {
    private val mutex = Mutex()
    private companion object {
        private const val MASTER_KEY_ALIAS = "idos_enclave_master"
        private const val KEY_FILENAME = "encrypted_key"

        fun EnclaveKeyType.filename() = "${KEY_FILENAME}_${this.name}"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey
            .Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(true) // Use StrongBox if available
            .build()
    }

    override suspend fun storeKey(
        key: ByteArray,
        enclaveKeyType: EnclaveKeyType,
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val file = File(context.filesDir, enclaveKeyType.filename())

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
    }

    override suspend fun retrieveKey(enclaveKeyType: EnclaveKeyType): ByteArray? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val file = File(context.filesDir, enclaveKeyType.filename())
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
        }

    override suspend fun deleteKey(enclaveKeyType: EnclaveKeyType) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val file = File(context.filesDir, enclaveKeyType.filename())
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    // Ignore deletion errors
                }
            }
        }
}
