package org.idos.enclave

class JvmMetadataStorage : MetadataStorage {
    private val metadataMap = mutableMapOf<EnclaveKeyType, KeyMetadata>()
    private var sessionConfig: MpcSessionConfig? = null

    override suspend fun store(
        meta: KeyMetadata,
        enclaveKeyType: EnclaveKeyType,
    ) {
        metadataMap[enclaveKeyType] = meta
    }

    override suspend fun get(enclaveKeyType: EnclaveKeyType): KeyMetadata? = metadataMap[enclaveKeyType]

    override suspend fun delete(enclaveKeyType: EnclaveKeyType) {
        metadataMap.remove(enclaveKeyType)
    }

    override suspend fun getSessionConfig(): MpcSessionConfig? = sessionConfig

    override suspend fun storeSessionConfig(config: MpcSessionConfig) {
        sessionConfig = config
    }
}
