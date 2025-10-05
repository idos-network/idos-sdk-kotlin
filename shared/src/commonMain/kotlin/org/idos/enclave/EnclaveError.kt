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
