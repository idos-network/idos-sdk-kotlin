package org.idos.enclave

import kotlinx.serialization.Serializable
import org.idos.getCurrentTimeMillis
import org.idos.kwil.rpc.UuidString

@Serializable
data class KeyMetadata(
    val userId: UuidString,
    val expiredAt: Long,
    val createdAt: Long = getCurrentTimeMillis(),
    val lastUsedAt: Long = getCurrentTimeMillis(),
)

sealed class EnclaveError(
    message: String,
) : Exception(message)

object KeyExpiredError : EnclaveError("Key expired")

object NoKeyError : EnclaveError("No key present, generate first")

// https://github.com/idos-network/idos-sdk-js/blob/main/packages/utils/src/enclave/local.ts
class Enclave(
    private val encryption: Encryption,
    private val storage: MetadataStorage,
) {
    suspend fun generateKey(
        userId: UuidString,
        password: String,
        expiration: Long,
    ) {
        val now = getCurrentTimeMillis()
        val meta = KeyMetadata(userId, now + expiration)
        encryption.deleteKey()
        encryption.generateKey(userId, password)
        storage.store(meta)
    }

    suspend fun deleteKey() {
        encryption.deleteKey()
        storage.delete()
    }

    /**
     * Decrypt credentials content
     */
    suspend fun decrypt(
        message: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray {
        val meta = expirationCheck()
        storage.store(meta.copy(lastUsedAt = getCurrentTimeMillis()))

        return encryption.decrypt(
            message,
            senderPublicKey,
        )
    }

    suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> {
        val meta = expirationCheck()
        storage.store(meta.copy(lastUsedAt = getCurrentTimeMillis()))

        return encryption.encrypt(message, receiverPublicKey)
    }

    /**
     * Check if the enclave has a valid (non-expired) key without performing encryption
     * Throws NoKeyError if no key exists, KeyExpiredError if key is expired
     */
    suspend fun hasValidKey() {
        expirationCheck()
    }

    private suspend fun expirationCheck(): KeyMetadata {
        val meta = storage.get() ?: throw NoKeyError

        if (meta.expiredAt < getCurrentTimeMillis()) {
            encryption.deleteKey()
            storage.delete()
            throw KeyExpiredError
        }

        return meta
    }
}
