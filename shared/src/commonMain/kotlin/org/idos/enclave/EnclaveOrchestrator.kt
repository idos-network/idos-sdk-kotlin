package org.idos.enclave

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.idos.kwil.types.UuidString

/**
 * Orchestrates enclave state transitions and operations.
 * Provides StateFlow for reactive UI updates across platforms.
 *
 * Thread-safe with Mutex-protected pending action queue.
 */
class EnclaveOrchestrator(
    private val enclave: Enclave,
) {
    private val _state = MutableStateFlow<EnclaveFlow>(EnclaveFlow.Loading)
    val state: StateFlow<EnclaveFlow> = _state.asStateFlow()

    private val pendingActions = mutableListOf<suspend (Enclave) -> Unit>()
    private val actionLock = Mutex()
    private var wrongPasswordAttempts = 0

    /**
     * Check if enclave has valid encryption key.
     * Transitions: Loading → Available | RequiresKey | Error
     */
    suspend fun checkStatus() {
        _state.value = EnclaveFlow.Loading

        enclave.hasValidKey()
            .onSuccess {
                wrongPasswordAttempts = 0
                _state.value = EnclaveFlow.Available(enclave)
            }
            .onFailure { error ->
                _state.value =
                    when (error) {
                        is EnclaveError.NoKey, is EnclaveError.KeyExpired -> EnclaveFlow.RequiresKey
                        else -> EnclaveFlow.Error(error.message ?: "Unknown error", canRetry = true)
                    }
            }
    }

    /**
     * User cancelled/dismissed key generation flow.
     * Clears pending actions.
     * Transitions: * → Cancelled
     */
    suspend fun cancel() =
        actionLock.withLock {
            pendingActions.clear()
            wrongPasswordAttempts = 0
            _state.value = EnclaveFlow.Cancelled
        }

    /**
     * Request enclave for operation.
     * If available: execute immediately
     * If not: queue for later execution after key generation
     *
     * @param action Suspending function that uses enclave (can throw)
     * @return Result indicating if action was executed or queued
     */
    suspend fun requireEnclave(
        action: suspend (Enclave) -> Unit,
    ): Result<Unit> {
        return when (val currentState = _state.value) {
            is EnclaveFlow.Available -> {
                executeAction(currentState.enclave, action)
            }

            is EnclaveFlow.Cancelled -> {
                // User cancelled - re-trigger flow and queue action
                actionLock.withLock {
                    pendingActions.add(action)
                }
                _state.value = EnclaveFlow.RequiresKey
                Result.failure(Exception("Enclave cancelled - action queued"))
            }

            else -> {
                // Not available - queue action and trigger check
                actionLock.withLock {
                    pendingActions.add(action)
                }
                checkStatus()
                Result.failure(Exception("Enclave not available - action queued"))
            }
        }
    }

    /**
     * Decrypt message using enclave.
     * Detects wrong password scenario.
     *
     * @return Result with decrypted bytes or error
     */
    suspend fun decrypt(
        message: ByteArray,
        senderPublicKey: ByteArray,
    ): Result<ByteArray> {
        return when (val currentState = _state.value) {
            is EnclaveFlow.Available -> {
                val result = currentState.enclave.decrypt(message, senderPublicKey)

                result
                    .onFailure { error ->
                        when (error) {
                            is EnclaveError.NoKey, is EnclaveError.KeyExpired -> {
                                // Expected errors - trigger key generation
                                _state.value = EnclaveFlow.RequiresKey
                            }

                            is EnclaveError.DecryptionFailed -> {
                                // Decryption failed with existing key → likely wrong password
                                wrongPasswordAttempts++
                                _state.value =
                                    EnclaveFlow.WrongPasswordSuspected(
                                        error.message ?: "Decryption failed",
                                        wrongPasswordAttempts,
                                    )
                            }

                            else -> {
                                _state.value =
                                    EnclaveFlow.Error(
                                        error.message ?: "Decryption error",
                                        canRetry = true,
                                    )
                            }
                        }
                    }.onSuccess {
                        // Successful decryption - reset counter
                        wrongPasswordAttempts = 0
                    }

                result
            }

            else -> {
                Result.failure(Exception("Enclave not available"))
            }
        }
    }

    /**
     * Encrypt message using enclave.
     *
     * @return Result with (ciphertext, nonce) or error
     */
    suspend fun encrypt(
        message: ByteArray,
        receiverPublicKey: ByteArray,
    ): Result<Pair<ByteArray, ByteArray>> {
        return when (val currentState = _state.value) {
            is EnclaveFlow.Available -> {
                val result = currentState.enclave.encrypt(message, receiverPublicKey)

                result
                    .onFailure { error ->
                        when (error) {
                            is EnclaveError.NoKey, is EnclaveError.KeyExpired -> {
                                _state.value = EnclaveFlow.RequiresKey
                            }

                            else -> {
                                _state.value =
                                    EnclaveFlow.Error(
                                        error.message ?: "Encryption failed",
                                        canRetry = true,
                                    )
                            }
                        }
                    }.onSuccess {
                        wrongPasswordAttempts = 0
                    }

                result
            }

            else -> {
                Result.failure(Exception("Enclave not available"))
            }
        }
    }

    /**
     * Generate encryption key.
     * Transitions: Generating → Available | KeyGenerationError
     */
    suspend fun generateKey(
        userId: UuidString,
        password: String,
        expirationMillis: Long,
    ) {
        _state.value = EnclaveFlow.Generating

        enclave.generateKey(userId, password, expirationMillis)
            .onSuccess {
                wrongPasswordAttempts = 0
                _state.value = EnclaveFlow.Available(enclave)

                // Execute pending actions
                executePendingActions(enclave)
            }
            .onFailure { error ->
                _state.value =
                    EnclaveFlow.KeyGenerationError(
                        error.message ?: "Key generation failed",
                    )
            }
    }

    /**
     * Delete encryption key and reset to initial state.
     * Use when password is wrong or user wants fresh start.
     * Transitions: * → RequiresKey
     */
    suspend fun reset() {
        actionLock.withLock {
            wrongPasswordAttempts = 0
            pendingActions.clear()
        }

        enclave.deleteKey()
            .onSuccess {
                _state.value = EnclaveFlow.RequiresKey
            }
            .onFailure { error ->
                _state.value =
                    EnclaveFlow.Error(
                        error.message ?: "Failed to reset",
                        canRetry = false,
                    )
            }
    }

    /**
     * Retry after error or wrong password.
     * For WrongPasswordSuspected: retries operation (maybe wrong data, not password)
     * For other errors: re-checks status
     */
    suspend fun retry() {
        when (_state.value) {
            is EnclaveFlow.WrongPasswordSuspected -> {
                // Don't automatically generate new key
                // User might have entered wrong credential data, not wrong password
                _state.value = EnclaveFlow.Available(enclave)
            }

            is EnclaveFlow.KeyGenerationError,
            is EnclaveFlow.Error,
            -> {
                checkStatus()
            }

            else -> {
                checkStatus()
            }
        }
    }

    private suspend fun executeAction(
        enclave: Enclave,
        action: suspend (Enclave) -> Unit,
    ): Result<Unit> =
        runCatching {
            action(enclave)
        }.onFailure { error ->
            when (error) {
                is EnclaveError.KeyExpired, is EnclaveError.NoKey -> {
                    // Queue for retry after new key generation
                    actionLock.withLock {
                        pendingActions.add(action)
                    }
                    _state.value = EnclaveFlow.RequiresKey
                }

                else -> {
                    // Don't queue - let app decide what to do
                    _state.value =
                        EnclaveFlow.Error(
                            error.message ?: "Action failed",
                            canRetry = true,
                        )
                }
            }
        }

    private suspend fun executePendingActions(enclave: Enclave) {
        val actions =
            actionLock.withLock {
                val copy = pendingActions.toList()
                pendingActions.clear()
                copy
            }

        for (action in actions) {
            runCatching {
                action(enclave)
            }.onFailure { error ->
                _state.value =
                    EnclaveFlow.Error(
                        error.message ?: "Pending action failed",
                        canRetry = true,
                    )
                return // Stop on first failure
            }
        }
    }
}
