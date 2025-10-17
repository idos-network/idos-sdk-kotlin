package org.idos.enclave

/**
 * Type-safe error hierarchy for enclave operations.
 * Provides exhaustive when expressions and iOS-compatible error handling.
 */
sealed class EnclaveError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /**
     * No encryption key exists - user must generate one.
     */
    class NoKey : EnclaveError("No encryption key present - generate key first")

    /**
     * Encryption key has expired.
     */
    class KeyExpired : EnclaveError("Encryption key expired")

    /**
     * Decryption operation failed.
     */
    data class DecryptionFailed(
        val reason: DecryptFailure,
        val details: String? = null,
    ) : EnclaveError(
            message = "Decryption failed: ${reason.description}" + (details?.let { " - $it" } ?: ""),
        )

    /**
     * Encryption operation failed.
     */
    data class EncryptionFailed(
        val details: String,
    ) : EnclaveError("Encryption failed: $details")

    /**
     * Secure storage operation failed.
     */
    data class StorageFailed(
        val details: String,
    ) : EnclaveError("Storage operation failed: $details")

    /**
     * Key generation failed.
     */
    data class KeyGenerationFailed(
        val details: String,
    ) : EnclaveError("Key generation failed: $details")

    /**
     * Sender or receiver public key is invalid.
     */
    data class InvalidPublicKey(
        val details: String,
    ) : EnclaveError("Invalid public key: $details")

    /**
     * MPC client not initialized - call initializeMpc() first.
     */
    class MpcNotInitialized : EnclaveError("MPC client not initialized - call initializeMpc() first")

    class MpcNotEnoughNodes(
        val details: String,
    ) : EnclaveError("MPC client initialized with not enough nodes: $details")

    data class MpcNotEnoughShares(
        val obtained: Int,
        val required: Int,
        val failures: List<MpcNodeFailure>,
    ) : EnclaveError(
            "Insufficient shares: got $obtained, need $required. " +
                "Failures: ${failures.joinToString(", ") { "Node ${it.nodeIndex}: ${it.error.message ?: "Unknown error"}" }}",
        )

    /**
     * MPC password operation failed.
     */
    data class MpcPasswordFailed(
        val details: String,
    ) : EnclaveError("MPC password operation failed: $details")

    /**
     * MPC download operation failed.
     */
    data class MpcDownloadFailed(
        val details: String,
    ) : EnclaveError("MPC download failed: $details")

    /**
     * MPC management operation failed (add/remove address, update wallets).
     */
    data class MpcManagementFailed(
        val operation: String,
        val successCount: Int,
        val required: Int,
        val failures: List<MpcNodeFailure>,
    ) : EnclaveError(
            "MPC $operation failed: got $successCount successes, need $required. " +
                "Failures: ${failures.joinToString(", ") { "Node ${it.nodeIndex}: ${it.error.message ?: "Unknown error"}" }}",
        )

    /**
     * MPC upload operation failed.
     */
    data class MpcUploadFailed(
        val successCount: Int,
        val required: Int,
        val failures: List<MpcNodeFailure>,
    ) : EnclaveError(
            "MPC upload failed: got $successCount successes, need $required. " +
                "Failures: ${failures.joinToString(", ") { "Node ${it.nodeIndex}: ${it.error.message ?: "Unknown error"}" }}",
        )

    /**
     * Signature operation failed.
     */
    data class SignatureFailed(
        val details: String,
    ) : EnclaveError("Signature failed: $details")

    /**
     * Generic enclave operation error.
     */
    data class Unknown(
        val details: String,
        override val cause: Throwable? = null,
    ) : EnclaveError("Enclave error: $details", cause)
}

/**
 * Specific reasons why decryption can fail.
 * Helps distinguish wrong password from corrupted data.
 */
sealed class DecryptFailure(
    val description: String,
) {
    /**
     * Likely wrong password used to generate encryption key.
     * Key exists and is valid, but decryption fails.
     */
    data object WrongPassword : DecryptFailure("Wrong password - key cannot decrypt this data")

    /**
     * Data appears corrupted or tampered with.
     */
    data object CorruptedData : DecryptFailure("Encrypted data is corrupted or invalid")

    /**
     * Ciphertext format is invalid.
     */
    data object InvalidCiphertext : DecryptFailure("Invalid ciphertext format")

    /**
     * Unknown decryption error.
     */
    data class Unknown(
        val message: String,
    ) : DecryptFailure(message)
}

/**
 * Represents a failure from a specific MPC node.
 *
 * @param nodeIndex Index of the node that failed
 * @param error The underlying error that occurred
 */
data class MpcNodeFailure(
    val nodeIndex: Int,
    val error: Throwable,
)

@PublishedApi
internal suspend inline fun <T> runCatchingErrorAsync(crossinline block: suspend () -> T): T =
    try {
        block()
    } catch (e: EnclaveError) {
        // Already a EnclaveError, pass through
        throw e
    } catch (e: Exception) {
        throw EnclaveError.Unknown(
            e.message ?: "An unexpected error occurred",
            cause = e,
        )
    }
