package org.idos.enclave

import org.idos.enclave.crypto.Encryption
import org.idos.getCurrentTimeMillis

/**
 * Shared expiration checking logic for both LOCAL and MPC enclaves.
 */
internal object ExpirationChecker {
    /**
     * Check if a key has expired and delete it if necessary.
     *
     * @param storage Metadata storage
     * @param encryption Encryption instance
     * @param keyType Type of key to check
     * @return KeyMetadata if valid
     * @throws EnclaveError.NoKey if no key is stored
     * @throws EnclaveError.KeyExpired if the stored key has expired
     */
    suspend fun check(
        storage: MetadataStorage,
        encryption: Encryption,
        keyType: EnclaveKeyType,
    ): KeyMetadata =
        runCatchingErrorAsync {
            val meta = storage.get(keyType) ?: throw EnclaveError.NoKey()
            val now = getCurrentTimeMillis()

            when (meta.expirationType) {
                ExpirationType.TIMED -> {
                    if (meta.expiresAt != null && now > meta.expiresAt) {
                        encryption.deleteKey(keyType)
                        storage.delete(keyType)
                        throw EnclaveError.KeyExpired()
                    }
                }
                ExpirationType.ONE_SHOT -> {
                    if (meta.lastUsedAt != meta.createdAt) {
                        // Already used once, auto-lock
                        encryption.deleteKey(keyType)
                        storage.delete(keyType)
                        throw EnclaveError.KeyExpired()
                    }
                }
                ExpirationType.SESSION -> {
                    // No auto-expiration, manual lock() required
                }
            }

            meta
        }
}
