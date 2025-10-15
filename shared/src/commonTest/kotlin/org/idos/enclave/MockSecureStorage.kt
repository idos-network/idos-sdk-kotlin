package org.idos.enclave

/**
 * Mock secure storage for testing encryption without platform dependencies.
 */
class MockSecureStorage : SecureStorage {
    private val keys = mutableMapOf<EnclaveKeyType, ByteArray>()

    override suspend fun storeKey(
        key: ByteArray,
        enclaveKeyType: EnclaveKeyType,
    ) {
        keys[enclaveKeyType] = key.copyOf()
    }

    override suspend fun retrieveKey(enclaveKeyType: EnclaveKeyType): ByteArray? = keys[enclaveKeyType]?.copyOf()

    override suspend fun deleteKey(enclaveKeyType: EnclaveKeyType) {
        keys[enclaveKeyType]?.fill(0)
        keys.remove(enclaveKeyType)
    }
}
