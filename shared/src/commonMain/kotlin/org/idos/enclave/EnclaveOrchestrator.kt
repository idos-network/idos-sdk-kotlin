package org.idos.enclave

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.idos.kwil.types.UuidString
import kotlin.coroutines.cancellation.CancellationException

/**
 * Orchestrates operations and state management of an `Enclave`.
 * Provides a reactive state model and encapsulates enclave operations such as locking, unlocking,
 * and executing actions securely using an encryption key.
 *
 * Manages transitions between the following states:
 * - `Locked`: No valid encryption key.
 * - `Unlocking`: Key generation or unlocking in progress.
 * - `Unlocked`: Key is ready for secure operations.
 *
 * Uses the `Enclave` object for encryption/decryption operations and key management.
 *
 * @property enclave The `Enclave` instance used for cryptographic operations.
 * @property state Reactive state flow representing the current state of the enclave.
 */
class EnclaveOrchestrator internal constructor(
    private val enclave: Enclave,
) {
    companion object {
        fun create(
            encryption: Encryption,
            storage: MetadataStorage,
        ): EnclaveOrchestrator = EnclaveOrchestrator(Enclave(encryption, storage))
    }

    private val _state = MutableStateFlow<EnclaveState>(EnclaveState.Locked)
    val state: StateFlow<EnclaveState> = _state.asStateFlow()

    /**
     * Returns the current state of the `EnclaveOrchestrator`.
     *
     * The state represents the current operational status of the enclave,
     * which can be updated or transitioned depending on operations performed.
     *
     * @return The current state value held by the orchestrator.
     */
    fun currentState() = state.value

    /**
     * Check if enclave has valid encryption key.
     * Updates state to Unlocked or Locked.
     */
    @Throws(CancellationException::class, EnclaveError::class)
    suspend fun checkStatus() {
        try {
            enclave.hasValidKey()
            _state.value = EnclaveState.Unlocked(enclave)
        } catch (e: EnclaveError) {
            _state.value = EnclaveState.Locked
        }
    }

    /**
     * Unlock enclave by generating encryption key.
     * Transitions: * → Unlocking → Unlocked | Locked
     *
     * @return Result with public key bytes or error
     */
    suspend fun unlock(
        userId: UuidString,
        password: String,
        expirationMillis: Long,
    ) {
        _state.value = EnclaveState.Unlocking
        try {
            enclave.generateKey(userId, password, expirationMillis)
            _state.value = EnclaveState.Unlocked(enclave)
        } catch (e: EnclaveError) {
            _state.value = EnclaveState.Locked
        }
    }

    /**
     * Lock enclave by deleting encryption key.
     * Transitions: * → Locked
     *
     * @return Result indicating success or failure
     */
    suspend fun lock() {
        try {
            enclave.deleteKey()
        } catch (e: EnclaveError) {
            // noop, lock anyway
        }
        _state.value = EnclaveState.Locked
    }

    /**
     * Execute action with enclave if unlocked.
     * No queuing - immediate execution or failure.
     *
     * @param action Suspending function that uses enclave and returns Result<T>
     * @return Result from action or NoKey error
     */
    suspend fun <T> withEnclave(action: suspend (Enclave) -> T): T =
        when (val currentState = _state.value) {
            is EnclaveState.Unlocked -> action(currentState.enclave)
            else -> throw EnclaveError.NoKey()
        }
}
