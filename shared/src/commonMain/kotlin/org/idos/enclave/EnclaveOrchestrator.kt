package org.idos.enclave

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.idos.kwil.types.UuidString
import kotlin.coroutines.cancellation.CancellationException

/**
 * Orchestrates Enclave operations with reactive state management.
 *
 * This is the **primary entry point** for all Enclave operations. It provides:
 * - Reactive state management via [StateFlow]
 * - Lifecycle handling (lock/unlock)
 * - State-aware operation execution
 * - Automatic expiration checks
 *
 * ## States:
 * - [EnclaveState.Locked] - No valid encryption key
 * - [EnclaveState.Unlocking] - Key generation in progress
 * - [EnclaveState.Unlocked] - Ready for encrypt/decrypt operations
 *
 * ## Kotlin Usage:
 * ```kotlin
 * // Create orchestrator
 * val encryption = JvmEncryption(JvmSecureStorage())
 * val storage = JvmMetadataStorage()
 * val orchestrator = EnclaveOrchestrator.create(encryption, storage)
 *
 * // Observe state changes
 * orchestrator.state.collect { state ->
 *     when (state) {
 *         is EnclaveState.Locked -> showPasswordPrompt()
 *         is EnclaveState.Unlocking -> showLoadingIndicator()
 *         is EnclaveState.Unlocked -> enableEncryptedFeatures()
 *     }
 * }
 *
 * // Unlock with password
 * orchestrator.unlock(
 *     userId = UuidString(userId),
 *     password = userPassword,
 *     expirationMillis = 3600000 // 1 hour
 * )
 *
 * // Decrypt data when unlocked
 * orchestrator.withEnclave { enclave ->
 *     enclave.decrypt(ciphertext, senderPublicKey)
 * }
 *
 * // Lock when done
 * orchestrator.lock()
 * ```
 *
 * ## Swift/iOS Usage:
 * ```swift
 * import idos_sdk
 *
 * // Create orchestrator with Keychain storage
 * let storage = KeychainSecureStorage()
 * let orchestrator = EnclaveOrchestrator.create(
 *     encryption: IosEncryption(storage: storage),
 *     metadataStorage: IosMetadataStorage()
 * )
 *
 * // Observe state with Combine
 * createPublisher(orchestrator.state)
 *     .receive(on: DispatchQueue.main)
 *     .sink { state in
 *         switch onEnum(of: state) {
 *         case .locked:
 *             showPasswordPrompt()
 *         case .unlocking:
 *             showLoadingIndicator()
 *         case .unlocked(let state):
 *             enableEncryptedFeatures()
 *         }
 *     }
 *
 * // Unlock
 * await orchestrator.unlock(
 *     userId: UuidString(value: userId),
 *     password: password,
 *     expirationMillis: 3600000
 * )
 *
 * // Decrypt when unlocked
 * let plaintext = try await orchestrator.withEnclave { enclave in
 *     try await enclave.decrypt(
 *         message: ciphertext,
 *         senderPublicKey: pubkey
 *     )
 * }
 * ```
 *
 * @property state Reactive state flow - observe for UI updates
 * @see EnclaveState for state definitions
 * @see EnclaveError for error types
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
     * Check if enclave has valid encryption key.
     * Updates state to Unlocked or Locked.
     */
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
