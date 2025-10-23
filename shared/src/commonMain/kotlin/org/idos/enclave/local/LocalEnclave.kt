package org.idos.enclave.local

import org.idos.enclave.Enclave
import org.idos.enclave.EnclaveError
import org.idos.enclave.EnclaveKeyType
import org.idos.enclave.EnclaveSessionConfig
import org.idos.enclave.ExpirationChecker
import org.idos.enclave.KeyMetadata
import org.idos.enclave.MetadataStorage
import org.idos.enclave.crypto.Encryption
import org.idos.enclave.runCatchingErrorAsync
import org.idos.getCurrentTimeMillis
import org.idos.kwil.types.UuidString
import kotlin.coroutines.cancellation.CancellationException

/**
 * Local Enclave for secure encryption/decryption operations using platform secure storage.
 *
 * **⚠️ Do not use directly - use [org.idos.enclave.EnclaveOrchestrator] instead.**
 *
 * This class should only be accessed through [org.idos.enclave.EnclaveOrchestrator], which provides:
 * - Reactive state management via Flow
 * - Proper lifecycle handling
 * - UI-friendly error states
 * - Automatic expiration checks
 *
 * @see org.idos.enclave.EnclaveOrchestrator for the public API and usage documentation
 * @see EnclaveError for error types and handling
 */
open class LocalEnclave internal constructor(
    private val encryption: Encryption,
    private val storage: MetadataStorage,
) : Enclave {
    /**
     * Generate new encryption key.
     *
     * @param userId User identifier for key derivation
     * @param password Password for key derivation
     * @param sessionConfig Session configuration (expiration type and duration) - required, no default
     * @return Public key bytes
     * @throws EnclaveError.KeyGenerationFailed if key generation fails
     */
    internal open suspend fun generateKey(
        userId: UuidString,
        password: String,
        sessionConfig: EnclaveSessionConfig,
    ): ByteArray =
        runCatchingErrorAsync {
            encryption.deleteKey(EnclaveKeyType.USER)
            val pubkey = encryption.generateKey(userId, password, EnclaveKeyType.USER)
            val now = getCurrentTimeMillis()
            val meta =
                KeyMetadata(
                    publicKey = pubkey.toHexString(),
                    type = EnclaveKeyType.USER,
                    expirationType = sessionConfig.expirationType,
                    expiresAt = sessionConfig.expirationMillis?.let { now + it },
                )
            storage.store(meta, EnclaveKeyType.USER)
            pubkey
        }

    /**
     * Delete encryption key.
     *
     * @throws EnclaveError.StorageFailed if key deletion fails
     */
    internal open suspend fun deleteKey(): Unit =
        runCatchingErrorAsync {
            encryption.deleteKey(EnclaveKeyType.USER)
            storage.delete(EnclaveKeyType.USER)
        }

    /**
     * Decrypt message.
     *
     * @param message Encrypted message bytes
     * @param senderPublicKey Sender's public key
     * @return Decrypted bytes
     * @throws EnclaveError.NoKey if no key is stored
     * @throws EnclaveError.KeyExpired if the stored key has expired
     * @throws EnclaveError.DecryptionFailed if decryption fails
     */
    @Throws(CancellationException::class, EnclaveError::class)
    override suspend fun decrypt(
        message: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray =
        runCatchingErrorAsync {
            val meta = expirationCheck()
            storage.store(meta.copy(lastUsedAt = getCurrentTimeMillis()), EnclaveKeyType.USER)
            encryption.decrypt(message, senderPublicKey, EnclaveKeyType.USER)
        }

    /**
     * Encrypt message.
     *
     * @param message Plain message bytes
     * @param receiverPublicKey Receiver's public key
     * @return Pair of (encrypted message with nonce, sender's public key)
     * @throws EnclaveError.NoKey if no key is stored
     * @throws EnclaveError.KeyExpired if the stored key has expired
     * @throws EnclaveError.EncryptionFailed if encryption fails
     */
    @Throws(CancellationException::class, EnclaveError::class)
    override suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray> =
        runCatchingErrorAsync {
            val meta = expirationCheck()
            storage.store(meta.copy(lastUsedAt = getCurrentTimeMillis()), EnclaveKeyType.USER)
            encryption.encrypt(message, receiverPublicKey, EnclaveKeyType.USER)
        }

    /**
     * Check if enclave has valid (non-expired) key.
     *
     * @throws EnclaveError.NoKey if no key is stored
     * @throws EnclaveError.KeyExpired if the stored key has expired
     */
    internal open suspend fun hasValidKey() {
        ExpirationChecker.check(storage, encryption, EnclaveKeyType.USER)
    }

    private suspend fun expirationCheck(): KeyMetadata = ExpirationChecker.check(storage, encryption, EnclaveKeyType.USER)
}
