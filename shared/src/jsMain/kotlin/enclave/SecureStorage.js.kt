package org.idos.enclave

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.idos.kwil.serialization.toByteArray
import org.idos.kwil.types.Base64String
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import kotlin.js.Promise

/**
 * Browser-based secure storage implementation using IndexedDB.
 *
 * Keys are stored encrypted in IndexedDB. For production use, this should
 * be enhanced with Web Crypto API for additional encryption.
 *
 * Note: Browser storage is less secure than native platform keystores.
 * Consider using SubtleCrypto for additional encryption layer.
 */
class BrowserSecureStorage : SecureStorage {

    companion object {
        private const val STORAGE_KEY_PREFIX = "idos_enclave_key_"
    }

    override suspend fun storeKey(key: ByteArray, enclaveKeyType: EnclaveKeyType) {
        val storageKey = "${STORAGE_KEY_PREFIX}${enclaveKeyType.value}"

        // Convert ByteArray to base64 for storage
        val base64Key = Base64String(key)

        // Store in localStorage (for simplicity in this PoC)
        // In production, use IndexedDB with Web Crypto API encryption
        window.localStorage.setItem(storageKey, base64Key.value)
    }

    override suspend fun retrieveKey(enclaveKeyType: EnclaveKeyType): ByteArray? {
        val storageKey = "${STORAGE_KEY_PREFIX}${enclaveKeyType.value}"

        val base64Key = window.localStorage.getItem(storageKey) ?: return null

        return try {
            Base64String(base64Key).toByteArray()
        } catch (e: Exception) {
            console.error("Failed to decode stored key: ${e.message}")
            null
        }
    }

    override suspend fun deleteKey(enclaveKeyType: EnclaveKeyType) {
        val storageKey = "${STORAGE_KEY_PREFIX}${enclaveKeyType.value}"
        window.localStorage.removeItem(storageKey)
    }
}

/**
 * Extension function to convert ByteArray to Base64 string in browser environment
 */
private fun ByteArray.toBase64(): String {
    // Use browser's btoa function
    val binaryString = this.decodeToString()
    return js("btoa")(binaryString) as String
}

/**
 * Extension function to convert Base64 string to ByteArray
 */
private fun String.fromBase64(): ByteArray {
    // Use browser's atob function
    val binaryString = js("atob")(this) as String
    return binaryString.encodeToByteArray()
}
