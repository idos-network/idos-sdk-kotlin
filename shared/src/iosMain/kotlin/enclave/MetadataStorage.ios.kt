package org.idos.enclave

import kotlinx.serialization.json.Json
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
    }

    override suspend fun store(meta: KeyMetadata) {
        val jsonString = json.encodeToString(KeyMetadata.serializer(), meta)
        userDefaults.setObject(jsonString, forKey = KEY_METADATA)
        userDefaults.synchronize()
    }

    override suspend fun get(): KeyMetadata? {
        val jsonString = userDefaults.stringForKey(KEY_METADATA) ?: return null
        return try {
            json.decodeFromString(KeyMetadata.serializer(), jsonString)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun delete() {
        userDefaults.removeObjectForKey(KEY_METADATA)
        userDefaults.synchronize()
    }
}
