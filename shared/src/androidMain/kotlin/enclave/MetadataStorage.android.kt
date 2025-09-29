package org.idos.enclave

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AndroidMetadataStorage(
    private val context: Context,
) : MetadataStorage {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("idos_key_metadata", Context.MODE_PRIVATE)
    }

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    override suspend fun store(meta: KeyMetadata) {
        val jsonString = json.encodeToString(meta)
        prefs
            .edit()
            .putString(KEY_METADATA, jsonString)
            .apply()
    }

    override suspend fun get(): KeyMetadata? {
        val jsonString = prefs.getString(KEY_METADATA, null) ?: return null

        return try {
            json.decodeFromString<KeyMetadata>(jsonString)
        } catch (e: Exception) {
            // Return null for corrupted data instead of throwing
            null
        }
    }

    override suspend fun delete() {
        prefs.edit().remove(KEY_METADATA).apply()
    }

    companion object {
        private const val KEY_METADATA = "key_metadata"
    }
}
