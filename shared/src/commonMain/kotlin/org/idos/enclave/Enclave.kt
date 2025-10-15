package org.idos.enclave

import kotlin.coroutines.cancellation.CancellationException

/**
 * Base interface for enclave operations.
 *
 * Provides common encrypt/decrypt operations that work across
 * both local (password-based) and MPC (network-based) enclaves.
 */
interface Enclave {
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
    suspend fun decrypt(
        message: ByteArray,
        senderPublicKey: ByteArray,
    ): ByteArray

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
    suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Pair<ByteArray, ByteArray>
}
