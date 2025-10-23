package org.idos.enclave

/**
 * Platform-specific secure storage for encryption keys.
 * Separates key persistence from encryption logic.
 *
 * Supports multiple keys via KeyType (LOCAL, MPC).
 */
interface SecureStorage {
    /**
     * Stores a secret key securely.
     * @param key The secret key bytes to store
     * @param enclaveKeyType Type of key being stored
     */
    suspend fun storeKey(
        key: ByteArray,
        enclaveKeyType: EnclaveKeyType,
    )

    /**
     * Retrieves the stored secret key.
     * @param enclaveKeyType Type of key to retrieve
     * @return The secret key bytes, or null if no key is stored
     */
    suspend fun retrieveKey(enclaveKeyType: EnclaveKeyType): ByteArray?

    /**
     * Deletes the stored secret key.
     * @param enclaveKeyType Type of key to delete
     */
    suspend fun deleteKey(enclaveKeyType: EnclaveKeyType)
}
