package org.idos.enclave

/**
 * Mock secure storage for testing encryption without platform dependencies.
 */
class MockSecureStorage : SecureStorage {
    private var key: ByteArray? = null

    override suspend fun storeKey(key: ByteArray) {
        this.key = key.copyOf()
    }

    override suspend fun retrieveKey(): ByteArray? = key?.copyOf()

    override suspend fun deleteKey() {
        key?.fill(0)
        key = null
    }
}
