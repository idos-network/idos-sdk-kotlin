package org.idos.enclave

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.idos.crypto.Keccak256Hasher
import org.idos.enclave.EnclaveOrchestrator.Companion.create
import org.idos.enclave.EnclaveOrchestrator.Companion.createLocal
import org.idos.enclave.EnclaveOrchestrator.Companion.createMpc
import org.idos.enclave.crypto.Encryption
import org.idos.enclave.local.LocalEnclave
import org.idos.enclave.mpc.MpcConfig
import org.idos.enclave.mpc.MpcEnclave
import org.idos.kwil.types.UuidString
import org.idos.signer.Signer
import kotlin.coroutines.cancellation.CancellationException

/**
 * Orchestrates LocalEnclave operations with reactive state management.
 *
 * This is the **primary entry point** for all Local Enclave operations. It provides:
 * - Reactive state management via [kotlinx.coroutines.flow.StateFlow]
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
 * val encryption = JvmEncryption(JvmSecureStorage(), KeyType.LOCAL)
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
 *     encryption: IosEncryption(storage: storage, keyType: KeyType.local),
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
    private val localEnclave: LocalEnclave?,
    private val mpcEnclave: MpcEnclave?,
    private val enclaveType: EnclaveKeyType?,
) {
    companion object Companion {
        /**
         * Create orchestrator for DI - requires all parameters for both LOCAL and MPC modes.
         * Type detection happens lazily based on storage metadata during checkStatus() or unlock().
         *
         * This is the default factory method for consumer apps that support both LOCAL and MPC.
         * MPC nodes are fetched on-demand - no persistent connections or caching.
         *
         * @param encryption Platform encryption instance
         * @param storage Metadata storage
         * @param mpcConfig MPC configuration (URL, contract, nodes, threshold)
         * @param signer Wallet signer for MPC operations
         * @param hasher Keccak256 hasher for MPC share commitments
         * @return EnclaveOrchestrator ready for DI injection
         */
        fun create(
            encryption: Encryption,
            storage: MetadataStorage,
            mpcConfig: MpcConfig,
            signer: Signer,
            hasher: Keccak256Hasher,
        ): EnclaveOrchestrator =
            EnclaveOrchestrator(
                localEnclave = LocalEnclave(encryption, storage),
                mpcEnclave =
                    MpcEnclave(
                        encryption = encryption,
                        storage = storage,
                        mpcConfig = mpcConfig,
                        signer = signer,
                        hasher = hasher,
                    ),
                // Will be determined lazily
                enclaveType = null,
            )

        /**
         * Create orchestrator for LOCAL mode only.
         * For consumer apps that only want password-based encryption.
         *
         * @param encryption Platform encryption instance
         * @param storage Metadata storage
         * @return EnclaveOrchestrator configured for LOCAL mode
         */
        fun createLocal(
            encryption: Encryption,
            storage: MetadataStorage,
        ): EnclaveOrchestrator =
            EnclaveOrchestrator(
                localEnclave = LocalEnclave(encryption, storage),
                mpcEnclave = null,
                enclaveType = EnclaveKeyType.USER,
            )

        /**
         * Create orchestrator for MPC mode only.
         * For consumer apps that only want MPC-based encryption.
         *
         * MPC nodes are fetched on-demand - no persistent connections or caching.
         *
         * @param encryption Platform encryption instance
         * @param storage Metadata storage
         * @param mpcConfig MPC configuration (URL, contract, nodes, threshold)
         * @param signer Wallet signer
         * @param hasher Keccak256 hasher
         * @return EnclaveOrchestrator configured for MPC mode
         */
        fun createMpc(
            encryption: Encryption,
            storage: MetadataStorage,
            mpcConfig: MpcConfig,
            signer: Signer,
            hasher: Keccak256Hasher,
        ): EnclaveOrchestrator =
            EnclaveOrchestrator(
                localEnclave = null,
                mpcEnclave =
                    MpcEnclave(
                        encryption = encryption,
                        storage = storage,
                        mpcConfig = mpcConfig,
                        signer = signer,
                        hasher = hasher,
                    ),
                enclaveType = EnclaveKeyType.MPC,
            )
    }

    private val _state = MutableStateFlow<EnclaveState>(EnclaveState.Locked)
    val state: StateFlow<EnclaveState> = _state.asStateFlow()

    private var innerEnclaveType: EnclaveKeyType? = enclaveType

    // we want to keep the NA state instead of locked, resend on updated
    private fun updateState(newState: EnclaveState) {
        if (_state.value != EnclaveState.NotAvailable) {
            _state.value = newState
        } else {
            _state.value = EnclaveState.NotAvailable
        }
    }

    /**
     * Set the enclave type. Used in combination with [create] where the mode is defined based on active user data.
     * This is not necessary for [createMpc] or [createLocal] mode.
     *
     * @param type The enclave type to set
     */
    fun initializeType(type: EnclaveKeyType) {
        innerEnclaveType = type
        // reset state to locked, overwriting NA if it was set by earlier call to checkStatus
        _state.value = EnclaveState.Locked
    }

    fun getEnclaveType(): EnclaveKeyType {
        require(innerEnclaveType != null) { "Enclave type not set" }
        return innerEnclaveType!!
    }

    /**
     * Enroll user into chosen enclave type during onboarding.
     *
     * @param userId User identifier
     * @param type Enclave type chosen by user (LOCAL or MPC)
     * @throws EnclaveError.MpcUploadFailed if MPC enrollment fails
     */
    @Throws(CancellationException::class, EnclaveError::class)
    suspend fun enroll(
        userId: UuidString,
        type: EnclaveKeyType,
    ) {
        runCatchingErrorAsync {
            when (type) {
                EnclaveKeyType.USER -> {
                    // Set type to LOCAL, state remains Locked (needs password unlock)
                    innerEnclaveType = EnclaveKeyType.USER
                    _state.value = EnclaveState.Locked
                }

                EnclaveKeyType.MPC -> {
                    requireNotNull(mpcEnclave) { "MPC enclave not initialized" }

                    updateState(EnclaveState.Unlocking)
                    try {
                        // Generate password and upload to MPC network
                        mpcEnclave.enroll(userId)

                        // Set type and state
                        innerEnclaveType = EnclaveKeyType.MPC
                        updateState(EnclaveState.Unlocked(mpcEnclave))
                    } catch (e: EnclaveError) {
                        _state.value = EnclaveState.Locked
                        throw e
                    }
                }
            }
        }
    }

    /**
     * Check if enclave has valid encryption key.
     * Updates state to Unlocked or Locked.
     */
    suspend fun checkStatus() {
        try {
            when (innerEnclaveType) {
                EnclaveKeyType.USER -> {
                    requireNotNull(localEnclave) { "LOCAL enclave not initialized" }
                    localEnclave.hasValidKey()
                    updateState(EnclaveState.Unlocked(localEnclave))
                }

                EnclaveKeyType.MPC -> {
                    requireNotNull(mpcEnclave) { "MPC enclave not initialized" }
                    mpcEnclave.hasValidKey()
                    updateState(EnclaveState.Unlocked(mpcEnclave))
                }

                null -> {
                    // No type set yet - remain locked
                    updateState(EnclaveState.NotAvailable)
                }
            }
        } catch (e: EnclaveError) {
            updateState(EnclaveState.Locked)
        }
    }

    @Throws(CancellationException::class, EnclaveError::class)
    suspend fun unlock(
        userId: UuidString,
        sessionConfig: EnclaveSessionConfig,
        password: String? = null,
    ) {
        if (password == null) {
            unlock(userId, sessionConfig)
        } else {
            unlock(userId, password, sessionConfig)
        }
    }

    /**
     * Unlock LOCAL enclave by generating encryption key from password.
     * Transitions: * → Unlocking → Unlocked | Locked
     *
     * Session config is read from storage (returns SESSION by default).
     *
     * @param userId User identifier for key derivation
     * @param password Password for key derivation
     * @param sessionConfig Session configuration (expiration type and duration) - required, no default
     * @throws IllegalArgumentException if orchestrator is configured for MPC mode only
     */
    internal suspend fun unlock(
        userId: UuidString,
        password: String,
        sessionConfig: EnclaveSessionConfig,
    ) {
        require(innerEnclaveType != EnclaveKeyType.MPC) {
            "Cannot unlock with password when using MPC mode - use unlock(userId) without password"
        }
        requireNotNull(localEnclave) { "LOCAL enclave not initialized" }

        updateState(EnclaveState.Unlocking)
        try {
            localEnclave.generateKey(userId, password, sessionConfig)
            updateState(EnclaveState.Unlocked(localEnclave))
        } catch (e: EnclaveError) {
            updateState(EnclaveState.Locked)
        }
    }

    /**
     * Unlock MPC enclave by downloading secret from network.
     * Transitions: * → Unlocking → Unlocked | Locked
     *
     * @param userId User identifier for the secret
     * @throws IllegalArgumentException if orchestrator is configured for LOCAL mode only
     * @throws EnclaveError.MpcNotInitialized if MPC client not initialized
     * @throws EnclaveError.MpcDownloadFailed if download fails
     */
    internal suspend fun unlock(
        userId: UuidString,
        sessionConfig: EnclaveSessionConfig,
    ) {
        require(innerEnclaveType != EnclaveKeyType.USER) {
            "Cannot unlock without password when using LOCAL mode - use unlock(userId, password, sessionConfig)"
        }
        requireNotNull(mpcEnclave) { "MPC enclave not initialized" }

        updateState(EnclaveState.Unlocking)
        try {
            mpcEnclave.unlock(userId, sessionConfig)
            updateState(EnclaveState.Unlocked(mpcEnclave))
        } catch (e: EnclaveError) {
            updateState(EnclaveState.Locked)
            throw e
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
            when (innerEnclaveType) {
                EnclaveKeyType.USER -> localEnclave?.deleteKey()
                EnclaveKeyType.MPC -> mpcEnclave?.deleteKey()
                null -> {
                    // Try both
                    localEnclave?.deleteKey()
                    mpcEnclave?.deleteKey()
                }
            }
        } catch (e: EnclaveError) {
            // noop, lock anyway
        }
        updateState(EnclaveState.Locked)
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
