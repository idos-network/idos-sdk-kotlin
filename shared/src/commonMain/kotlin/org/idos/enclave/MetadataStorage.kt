package org.idos.enclave

/**
 * Platform-specific metadata storage for enclave keys.
 *
 * Supports multiple keys via KeyType (LOCAL, MPC).
 * Also stores MPC session configuration.
 */
interface MetadataStorage {
    /**
     * Store key metadata.
     * @param meta Key metadata to store
     * @param enclaveKeyType Type of key
     */
    suspend fun store(
        meta: KeyMetadata,
        enclaveKeyType: EnclaveKeyType,
    )

    /**
     * Retrieve key metadata.
     * @param enclaveKeyType Type of key
     * @return Key metadata, or null if not found
     */
    suspend fun get(enclaveKeyType: EnclaveKeyType): KeyMetadata?

    /**
     * Delete key metadata.
     * @param enclaveKeyType Type of key
     */
    suspend fun delete(enclaveKeyType: EnclaveKeyType)
}
