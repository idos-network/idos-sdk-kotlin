package org.idos.enclave

import org.idos.getCurrentTimeMillis
import org.idos.kwil.types.HexString
import org.idos.kwil.types.UuidString

/**
 * Mock Enclave for testing EnclaveOrchestrator.
 * Allows simulating various states and error conditions.
 */
class MockEnclave(
    private val behavior: MockBehavior = MockBehavior.Success,
) : Enclave(
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
        println("*****************")
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
    ): Pair<ByteArray, ByteArray> {
        val secret = getSecretKey()
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
    ): ByteArray {
        val secret = getSecretKey()
        // XOR is symmetric, so decrypt = encrypt
        return fullMessage
            .mapIndexed { i, byte ->
                (byte.toInt() xor secret[i % secret.size].toInt()).toByte()
            }.toByteArray()
    }

    override suspend fun publicKey(secret: ByteArray): ByteArray = ByteArray(32) { i -> (secret[i % secret.size].toInt() * 2).toByte() }
}

/**
 * Mock MetadataStorage for testing.
 */
internal class MockMetadataStorage : MetadataStorage {
    private var metadata: KeyMetadata? = null

    override suspend fun store(meta: KeyMetadata) {
        this.metadata = meta
    }

    override suspend fun get(): KeyMetadata? = metadata

    override suspend fun delete() {
        metadata = null
    }
}
