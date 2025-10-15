package org.idos.enclave

import org.idos.enclave.crypto.Encryption
import org.idos.enclave.local.LocalEnclave
import org.idos.kwil.types.UuidString

/**
 * Mock Enclave for testing EnclaveOrchestrator.
 * Allows simulating various states and error conditions.
 */
class MockEnclave(
    private val behavior: MockBehavior = MockBehavior.Success,
) : LocalEnclave(
        encryption = MockEncryption(MockSecureStorage()),
        storage = MockMetadataStorage(),
    ) {
    sealed class MockBehavior {
        /** All operations succeed */
        data object Success : MockBehavior()

        /** Generate key fails */
        data class KeyGenerationFails(
            val error: EnclaveError,
        ) : MockBehavior()

        /** Decrypt fails (simulates wrong password) */
        data class DecryptFails(
            val error: EnclaveError,
        ) : MockBehavior()

        /** Encrypt fails */
        data class EncryptFails(
            val error: EnclaveError,
        ) : MockBehavior()

        /** Delete key fails */
        data object DeleteKeyFails : MockBehavior()

        /** Key expired */
        data object KeyExpired : MockBehavior()

        /** No key present */
        data object NoKey : MockBehavior()
    }

    var generateKeyCallCount = 0
        private set
    var deleteKeyCallCount = 0
        private set
    var decryptCallCount = 0
        private set
    var encryptCallCount = 0
        private set
    var hasValidKeyCallCount = 0
        private set

    override suspend fun generateKey(
        userId: UuidString,
        password: String,
        expiration: Long,
    ): ByteArray {
        generateKeyCallCount++
        return when (behavior) {
            is MockBehavior.KeyGenerationFails -> throw behavior.error
            else -> super.generateKey(userId, password, expiration)
        }
    }

    override suspend fun deleteKey() {
        deleteKeyCallCount++
        when (behavior) {
            is MockBehavior.DeleteKeyFails -> throw EnclaveError.StorageFailed("Delete failed")
            else -> super.deleteKey()
        }
    }

    override suspend fun decrypt(
        message: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray {
        decryptCallCount++
        return when (behavior) {
            is MockBehavior.DecryptFails -> throw behavior.error
            MockBehavior.NoKey -> throw EnclaveError.NoKey()
            MockBehavior.KeyExpired -> throw EnclaveError.KeyExpired()
            else -> super.decrypt(message, senderPublicKey)
        }
    }

    override suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> {
        encryptCallCount++
        return when (behavior) {
            is MockBehavior.EncryptFails -> throw behavior.error
            MockBehavior.NoKey -> throw EnclaveError.NoKey()
            MockBehavior.KeyExpired -> throw EnclaveError.KeyExpired()
            else -> super.encrypt(message, receiverPublicKey)
        }
    }

    override suspend fun hasValidKey() {
        hasValidKeyCallCount++
        when (behavior) {
            MockBehavior.NoKey -> throw EnclaveError.NoKey()
            MockBehavior.KeyExpired -> throw EnclaveError.KeyExpired()
            else -> super.hasValidKey()
        }
    }

    fun resetCallCounts() {
        generateKeyCallCount = 0
        deleteKeyCallCount = 0
        decryptCallCount = 0
        encryptCallCount = 0
        hasValidKeyCallCount = 0
    }
}

/**
 * Mock Encryption for testing.
 * Uses simple XOR "encryption" for deterministic testing.
 */
internal class MockEncryption(
    storage: SecureStorage,
) : Encryption(storage) {
    override suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
        enclaveKeyType: EnclaveKeyType,
    ): Pair<ByteArray, ByteArray> {
        val secret = getSecretKey(EnclaveKeyType.LOCAL)
        val nonce = ByteArray(24) { it.toByte() } // Deterministic nonce
        val encrypted =
            message
                .mapIndexed { i, byte ->
                    (byte.toInt() xor secret[i % secret.size].toInt()).toByte()
                }.toByteArray()
        val pubkey = publicKey(secret)
        return encrypted to pubkey
    }

    override suspend fun decrypt(
        fullMessage: ByteArray,
        senderPublicKey: ByteArray,
        enclaveKeyType: EnclaveKeyType,
    ): ByteArray {
        val secret = getSecretKey(EnclaveKeyType.LOCAL)
        // XOR is symmetric, so decrypt = encrypt
        return decrypt(fullMessage, secret, senderPublicKey)
    }

    override suspend fun decrypt(
        fullMessage: ByteArray,
        secret: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray {
        // XOR is symmetric, so decrypt = encrypt
        return fullMessage
            .mapIndexed { i, byte ->
                (byte.toInt() xor secret[i % secret.size].toInt()).toByte()
            }.toByteArray()
    }

    override suspend fun publicKey(secret: ByteArray): ByteArray = ByteArray(32) { i -> (secret[i % secret.size].toInt() * 2).toByte() }

    override fun generateEphemeralKeyPair(): org.idos.enclave.crypto.KeyPair {
        val pubKey = ByteArray(32) { it.toByte() }
        val secretKey = ByteArray(32) { (it * 2).toByte() }
        return org.idos.enclave.crypto
            .KeyPair(pubKey, secretKey)
    }
}

/**
 * Mock MetadataStorage for testing.
 */
internal class MockMetadataStorage : MetadataStorage {
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
