package org.idos.enclave

class JvmMetadataStorage : MetadataStorage {
    private var metadata: KeyMetadata? = null

    override suspend fun store(meta: KeyMetadata) {
        metadata = meta
    }

    override suspend fun get(): KeyMetadata? = metadata

    override suspend fun delete() {
        metadata = null
    }
}
