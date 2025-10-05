package org.idos.enclave

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.idos.kwil.types.UuidString

/**
 * Simple enclave state tracker for reactive UI updates.
 * No action queuing, no retry logic - just state management.
 * ViewModel controls orchestration logic.
 */
class EnclaveOrchestrator(
    private val enclave: Enclave,
) {
    private val _state = MutableStateFlow<EnclaveState>(EnclaveState.Locked)
    val state: StateFlow<EnclaveState> = _state.asStateFlow()

    /**
     * Check if enclave has valid encryption key.
     * Updates state to Unlocked or Locked.
     */
    suspend fun checkStatus() {
        enclave
            .hasValidKey()
            .onSuccess {
                _state.value = EnclaveState.Unlocked(enclave)
            }.onFailure {
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
    ): Result<Unit> {
        _state.value = EnclaveState.Unlocking

        return enclave
            .generateKey(userId, password, expirationMillis)
            .map { }
            .onSuccess {
                _state.value = EnclaveState.Unlocked(enclave)
            }.onFailure {
                _state.value = EnclaveState.Locked
            }
    }

    /**
     * Lock enclave by deleting encryption key.
     * Transitions: * → Locked
     *
     * @return Result indicating success or failure
     */
    suspend fun lock(): Result<Unit> =
        enclave
            .deleteKey()
            .onSuccess {
                _state.value = EnclaveState.Locked
            }.onFailure {
                // Even if delete fails, treat as locked
                _state.value = EnclaveState.Locked
            }

    /**
     * Execute action with enclave if unlocked.
     * No queuing - immediate execution or failure.
     *
     * @param action Suspending function that uses enclave and returns Result<T>
     * @return Result from action or NoKey error
     */
    suspend fun <T> withEnclave(action: suspend (Enclave) -> Result<T>): Result<T> =
        when (val currentState = _state.value) {
            is EnclaveState.Unlocked -> action(currentState.enclave)
            else -> Result.failure(EnclaveError.NoKey())
        }
}
