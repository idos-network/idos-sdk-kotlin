package org.idos.enclave.crypto

import org.idos.enclave.EnclaveError
import org.idos.enclave.EnclaveKeyType
import org.idos.enclave.SecureStorage
import org.idos.enclave.crypto.KeyDerivation.Companion.deriveKey
import org.idos.kwil.types.UuidString

// https://github.com/idos-network/idos-sdk-js/blob/main/packages/utils/src/encryption/index.ts
abstract class Encryption(
    protected val storage: SecureStorage,
) {
    companion object {
        fun keyDerivation(
            password: String,
            salt: String,
        ): ByteArray = deriveKey(password, salt)
    }

    /**
     * Generate ephemeral NaCl key pair for MPC download operations.
     * Platform-specific implementation.
     * @return KeyPair with 32-byte public and secret keys
     */
    abstract fun generateEphemeralKeyPair(): KeyPair

    internal abstract suspend fun publicKey(secret: ByteArray): ByteArray

    internal abstract suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
        enclaveKeyType: EnclaveKeyType,
    ): Pair<ByteArray, ByteArray>

    internal abstract suspend fun decrypt(
        fullMessage: ByteArray,
        senderPublicKey: ByteArray,
        enclaveKeyType: EnclaveKeyType,
    ): ByteArray

    internal abstract suspend fun decrypt(
        fullMessage: ByteArray,
        secret: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray

    internal suspend fun generateKey(
        userId: UuidString,
        password: String,
        enclaveKeyType: EnclaveKeyType,
    ): ByteArray =
        runCatching {
            val secretKey = keyDerivation(password, userId)
            storage.storeKey(secretKey, enclaveKeyType)
            val pubkey = publicKey(secretKey)
            secretKey.fill(0)
            pubkey
        }.getOrElse { error ->
            throw EnclaveError.KeyGenerationFailed(error.message ?: "Unknown error")
        }

    internal suspend fun deleteKey(enclaveKeyType: EnclaveKeyType) =
        runCatching {
            storage.deleteKey(enclaveKeyType)
        }.getOrElse { error ->
            throw EnclaveError.StorageFailed(error.message ?: "Failed to delete key")
        }

    /**
     * Store a key directly in secure storage (for MPC use case).
     * Used when key is retrieved from external source (MPC) and needs to be temporarily stored.
     */
    internal suspend fun storeKey(key: ByteArray) =
        runCatching {
            storage.storeKey(key, EnclaveKeyType.MPC)
        }.getOrElse { error ->
            throw EnclaveError.StorageFailed(error.message ?: "Failed to store key")
        }

    internal suspend fun getSecretKey(enclaveKeyType: EnclaveKeyType): ByteArray =
        storage.retrieveKey(enclaveKeyType) ?: throw EnclaveError.NoKey()
}
