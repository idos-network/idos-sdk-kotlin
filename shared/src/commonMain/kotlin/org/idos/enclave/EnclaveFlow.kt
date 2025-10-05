package org.idos.enclave

/**
 * Represents the state of the enclave orchestration flow.
 * Used with StateFlow for reactive UI updates across platforms.
 */
sealed class EnclaveFlow {
    /**
     * Checking if encryption key exists and is valid.
     */
    data object Loading : EnclaveFlow()

    /**
     * No key present or key expired - user must generate new key.
     */
    data object RequiresKey : EnclaveFlow()

    /**
     * User cancelled or dismissed key generation flow.
     */
    data object Cancelled : EnclaveFlow()

    /**
     * Encryption key is being generated.
     */
    data object Generating : EnclaveFlow()

    /**
     * Enclave ready for encrypt/decrypt operations.
     *
     * @param enclave The active enclave instance
     */
    data class Available(val enclave: Enclave) : EnclaveFlow()

    /**
     * Key generation failed.
     *
     * @param message Error message describing what went wrong
     */
    data class KeyGenerationError(val message: String) : EnclaveFlow()

    /**
     * Decryption failed with existing valid key - likely wrong password.
     * Prevents infinite retry loop.
     *
     * @param message Error description
     * @param attemptCount Number of consecutive failed attempts
     */
    data class WrongPasswordSuspected(
        val message: String,
        val attemptCount: Int = 1,
    ) : EnclaveFlow()

    /**
     * General error occurred.
     *
     * @param message Error description
     * @param canRetry Whether retry is possible
     */
    data class Error(
        val message: String,
        val canRetry: Boolean = true,
    ) : EnclaveFlow()
}
