package org.idos.enclave

interface MetadataStorage {
    suspend fun store(meta: KeyMetadata)

    suspend fun get(): KeyMetadata?
}
