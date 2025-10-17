package org.idos.enclave

/**
 * Represents the state of the enclave.
 * Simple 3-state model for reactive UI updates across platforms.
 */
sealed class EnclaveState {
    // TODO add N/A, in case it was not properly initialized, would show some warning to user

    /**
     * Enclave not available for the user.
     * We were not able to initialize properly.
     */
    data object NotAvailable : EnclaveState()

    /**
     * No encryption key or key expired.
     * User must unlock enclave with password or MPC.
     */
    data object Locked : EnclaveState()

    /**
     * Encryption key is being generated.
     * User is unlocking the enclave.
     */
    data object Unlocking : EnclaveState()

    /**
     * Enclave ready for encrypt/decrypt operations.
     *
     * @param enclave The active enclave instance
     */
    data class Unlocked(
        val enclave: Enclave,
    ) : EnclaveState()
}
