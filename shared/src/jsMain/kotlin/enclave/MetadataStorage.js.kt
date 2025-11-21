package org.idos.enclave

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Browser-based metadata storage implementation using localStorage.
 *
 * Stores key metadata as JSON in localStorage.
 */
class BrowserMetadataStorage : MetadataStorage {

    companion object {
        private const val METADATA_KEY_PREFIX = "idos_enclave_metadata_"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    override suspend fun store(meta: KeyMetadata, enclaveKeyType: EnclaveKeyType) {
        val storageKey = "${METADATA_KEY_PREFIX}${enclaveKeyType.value}"
        val jsonString = json.encodeToString(meta)
        window.localStorage.setItem(storageKey, jsonString)
    }

    override suspend fun get(enclaveKeyType: EnclaveKeyType): KeyMetadata? {
        val storageKey = "${METADATA_KEY_PREFIX}${enclaveKeyType.value}"
        val jsonString = window.localStorage.getItem(storageKey) ?: return null

        return try {
            json.decodeFromString<KeyMetadata>(jsonString)
        } catch (e: Exception) {
            console.error("Failed to decode metadata: ${e.message}")
            null
        }
    }

    override suspend fun delete(enclaveKeyType: EnclaveKeyType) {
        val storageKey = "${METADATA_KEY_PREFIX}${enclaveKeyType.value}"
        window.localStorage.removeItem(storageKey)
    }
}
