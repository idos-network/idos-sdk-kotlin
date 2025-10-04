package org.idos.enclave

/**
 * Platform-specific secure storage for encryption keys.
 * Separates key persistence from encryption logic.
 */
interface SecureStorage {
    /**
     * Stores a secret key securely.
     * @param key The secret key bytes to store
     */
    suspend fun storeKey(key: ByteArray)

    /**
     * Retrieves the stored secret key.
     * @return The secret key bytes, or null if no key is stored
     */
    suspend fun retrieveKey(): ByteArray?

    /**
     * Deletes the stored secret key.
     */
    suspend fun deleteKey()
}
