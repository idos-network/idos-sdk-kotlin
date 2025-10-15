package org.idos.enclave

import kotlinx.serialization.Serializable
import org.idos.getCurrentTimeMillis
import org.idos.kwil.types.HexString
import org.idos.kwil.types.UuidString

/**
 * Metadata for encryption keys stored in the enclave.
 *
 * @param userId User identifier for the key
 * @param publicKey Public key in hex format
 * @param type Type of key storage (LOCAL or MPC)
 * @param expirationType How the key expires (TIMED, SESSION, ONE_SHOT)
 * @param expiresAt Expiration timestamp (required for TIMED, null otherwise)
 * @param createdAt Timestamp when key was created
 * @param lastUsedAt Timestamp when key was last used
 */
@Serializable
data class KeyMetadata(
    val userId: UuidString,
    val publicKey: HexString,
    val type: EnclaveKeyType,
    val expirationType: ExpirationType,
    val expiresAt: Long? = null,
    val createdAt: Long = getCurrentTimeMillis(),
    val lastUsedAt: Long = getCurrentTimeMillis(),
)

/**
 * Key expiration strategy.
 */
@Serializable
enum class ExpirationType {
    /**
     * Key expires after a specified duration.
     * Requires `expiresAt` timestamp in metadata.
     */
    TIMED,

    /**
     * Key persists until manual lock() or app restart.
     * Consumer app must explicitly call lock() to clear.
     */
    SESSION,

    /**
     * Key auto-locks after first use.
     * Checks if lastUsedAt != createdAt to determine if used.
     */
    ONE_SHOT,
}

/**
 * Type of key storage backend.
 */
@Serializable
enum class EnclaveKeyType {
    /**
     * Key stored in platform secure storage (iOS Keychain, Android KeyStore, etc.)
     */
    LOCAL,

    /**
     * Key stored distributed across MPC network using Shamir's Secret Sharing
     */
    MPC,
}

/**
 * Runtime configuration for MPC enclave session management.
 *
 * Controls how long encryption keys remain unlocked.
 * Consumer app manages persistence via MetadataStorage.
 *
 * @param expirationType How the key should expire
 * @param expirationMillis Duration in milliseconds (required for TIMED, ignored otherwise)
 */
@Serializable
data class MpcSessionConfig(
    val expirationType: ExpirationType = ExpirationType.SESSION,
    val expirationMillis: Long? = null,
) {
    init {
        require(expirationType != ExpirationType.TIMED || expirationMillis != null) {
            "expirationMillis is required when expirationType is TIMED"
        }
        if (expirationMillis != null) {
            require(expirationMillis > 0) {
                "expirationMillis must be positive"
            }
        }
    }
}
