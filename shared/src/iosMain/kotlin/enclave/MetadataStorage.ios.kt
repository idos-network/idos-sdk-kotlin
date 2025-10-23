package org.idos.enclave

import kotlinx.serialization.json.Json
import org.idos.logging.IdosLogger
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of MetadataStorage using UserDefaults
 * Matches the Android implementation using SharedPreferences
 */
class IosMetadataStorage : MetadataStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val KEY_METADATA = "enclave_key_metadata"

        private fun EnclaveKeyType.key() = "${KEY_METADATA}_${this.name}"
    }

    override suspend fun store(
        meta: KeyMetadata,
        enclaveKeyType: EnclaveKeyType,
    ) {
        val jsonString = json.encodeToString(KeyMetadata.serializer(), meta)
        userDefaults.setObject(jsonString, forKey = enclaveKeyType.key())
        userDefaults.synchronize()
    }

    override suspend fun get(enclaveKeyType: EnclaveKeyType): KeyMetadata? {
        val jsonString = userDefaults.stringForKey(enclaveKeyType.key()) ?: return null
        return try {
            json.decodeFromString(KeyMetadata.serializer(), jsonString)
        } catch (e: Exception) {
            IdosLogger.e("MetadataStorage", e) {
                "Failed to deserialize metadata for $enclaveKeyType: ${e.message}"
            }
            throw EnclaveError.StorageFailed("Failed to deserialize metadata for $enclaveKeyType", e)
        }
    }

    override suspend fun delete(enclaveKeyType: EnclaveKeyType) {
        userDefaults.removeObjectForKey(enclaveKeyType.key())
        userDefaults.synchronize()
    }
}
