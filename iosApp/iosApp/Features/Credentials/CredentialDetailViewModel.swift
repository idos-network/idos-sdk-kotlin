import Foundation
import UIKit
import Combine
import OSLog
import idos_sdk

// MARK: - Enclave UI State

/// Enclave dialog/UI state (matches Android EnclaveUiState)
enum EnclaveUiState: Equatable {
    case hidden
    case requiresUnlock(type: EnclaveKeyType)
    case unlocking(type: EnclaveKeyType)
    case unlockError(type: EnclaveKeyType, message: String, canRetry: Bool)

    var type: EnclaveKeyType? {
        switch self {
        case .hidden:
            return nil
        case .requiresUnlock(let type), .unlocking(let type), .unlockError(let type, _, _):
            return type
        }
    }

    // Manual Equatable conformance because EnclaveKeyType is from Kotlin
    static func == (lhs: EnclaveUiState, rhs: EnclaveUiState) -> Bool {
        switch (lhs, rhs) {
        case (.hidden, .hidden):
            return true
        case (.requiresUnlock(let lType), .requiresUnlock(let rType)),
             (.unlocking(let lType), .unlocking(let rType)):
            return lType == rType
        case (.unlockError(let lType, let lMsg, let lRetry), .unlockError(let rType, let rMsg, let rRetry)):
            return lType == rType && lMsg == rMsg && lRetry == rRetry
        default:
            return false
        }
    }
}

// MARK: - Credential Detail State

/// Credential Detail state
struct CredentialDetailState {
    var credential: CredentialDetail?
    var decryptedContent: String? = nil  // nil = still encrypted
    var isLoading: Bool = false
    var isDecrypting: Bool = false
    var error: String? = nil
}

// MARK: - Credential Detail Events

/// Credential Detail events
enum CredentialDetailEvent {
    case loadCredential
    case decryptCredential
    case clearError

    // Enclave events
    case unlockEnclave(password: String?, config: EnclaveSessionConfig)
    case lockEnclave
    case dismissEnclave
}

// MARK: - ViewModel

/// CredentialDetailViewModel with direct orchestrator usage (matches Android pattern)
class CredentialDetailViewModel: BaseViewModel<CredentialDetailState, CredentialDetailEvent> {
    private let credentialId: String
    private let navigationCoordinator: NavigationCoordinator
    private let credentialsRepository: CredentialsRepositoryProtocol
    private let userRepository: UserRepositoryProtocol
    private let orchestrator: EnclaveOrchestrator

    @Published var enclaveUiState: EnclaveUiState = .hidden

    init(
        credentialId: String,
        orchestrator: EnclaveOrchestrator,
        credentialsRepository: CredentialsRepositoryProtocol,
        userRepository: UserRepositoryProtocol,
        navigationCoordinator: NavigationCoordinator
    ) {
        Logger.viewModel.debug("CredentialDetailViewModel: Initializing for credentialId: \(credentialId)")
        self.credentialId = credentialId
        self.navigationCoordinator = navigationCoordinator
        self.credentialsRepository = credentialsRepository
        self.userRepository = userRepository
        self.orchestrator = orchestrator

        super.init(initialState: CredentialDetailState())

        loadCredential()
        observeEnclaveState()
        
        // Check status on init
        Task {
            do {
                try await orchestrator.checkStatus()
            } catch {}
        }
    }

    override func onEvent(_ event: CredentialDetailEvent) {
        Logger.viewModel.debug("CredentialDetailViewModel: on event: \(String(describing: event))")
        switch event {
        case .loadCredential:
            loadCredential()
        case .decryptCredential:
            triggerDecrypt()
        case .clearError:
            state.error = nil
        case .unlockEnclave(let password, let config):
            unlockEnclave(password: password, config: config)
        case .lockEnclave:
            lockEnclave()
        case .dismissEnclave:
            dismissEnclave()
        }
    }

    // MARK: - Enclave State Observer

    private func observeEnclaveState() {
        Task { @MainActor in
            for await enclaveState: EnclaveState in orchestrator.state {
                Logger.enclave.debug("CredentialDetailViewModel: Observing enclave state: \(String(describing: type(of: enclaveState)))")
                switch enclaveState {
                case is EnclaveState.Locked:
                    // Don't automatically show dialog - wait for decrypt attempt
                    break

                case is EnclaveState.Unlocking:
                    self.enclaveUiState = .unlocking(type: orchestrator.getEnclaveType())

                case let unlocked as EnclaveState.Unlocked:
                    self.enclaveUiState = .hidden
                    // Auto-decrypt when unlocked
                    guard let credential = state.credential else { return }
                    await decryptLoadedCredential(enclave: unlocked.enclave, credential: credential)

                case is EnclaveState.NotAvailable:
                    // Enclave not available, don't show dialog
                    break

                default:
                    break
                }
            }
        }
    }

    // MARK: - Load Credential

    private func loadCredential() {
        Logger.viewModel.debug("CredentialDetailViewModel: Starting loadCredential for id: \(self.credentialId)")
        Task { @MainActor [weak self] in
            guard let self = self else { return }

            self.state.isLoading = true
            self.state.error = nil
            
            do {
                let detail = try await credentialsRepository.getCredential(id: credentialId)

                let currentState = orchestrator.state.value
                if currentState is EnclaveState.Unlocked {
                    let unlocked = currentState as! EnclaveState.Unlocked
                    await decryptLoadedCredential(enclave: unlocked.enclave, credential: detail)
                }
                
                self.state.credential = detail
                self.state.isLoading = false
            } catch {
                Logger.viewModel.error("CredentialDetailViewModel: Load failed - \(error.localizedDescription)")
                self.state.error = "Failed to load credential: \(error.localizedDescription)"
                self.state.isLoading = false
            }
        }
    }

    // MARK: - Decrypt

    private func triggerDecrypt() {
        guard let credential = state.credential else { return }
        // Check if enclave is unlocked
        self.state.isDecrypting = true
        if orchestrator.state.value is EnclaveState.Unlocked {
            let unlocked = orchestrator.state.value as! EnclaveState.Unlocked
            Task {
                await decryptLoadedCredential(enclave: unlocked.enclave, credential: credential)
            }
        } else if orchestrator.state.value is EnclaveState.Locked {
            // Show unlock dialog
            enclaveUiState = .requiresUnlock(type: orchestrator.getEnclaveType())
        } else {
            // Enclave not available
            self.state.error = "Encryption key not available"
            self.state.isDecrypting = false
        }
    }

    private func decryptLoadedCredential(enclave: Enclave, credential: CredentialDetail) async {
        guard state.decryptedContent == nil else { return } // Already decrypted

        Logger.enclave.debug("CredentialDetailViewModel: Starting decryption")

        let encryptedData = Data(base64Encoded: credential.content) ?? Data()
        let senderPublicKey = Data(base64Encoded: credential.encryptorPublicKey) ?? Data()

        do {
            let decryptedData = try await enclave.decrypt(
                message: encryptedData.toKotlinByteArray(),
                senderPublicKey: senderPublicKey.toKotlinByteArray()
            )
            
            if let decryptedContent = String(data: decryptedData.toNSData(), encoding: .utf8) {
                await MainActor.run {
                    self.state.decryptedContent = decryptedContent
                    self.state.isDecrypting = false
                    Logger.enclave.info("CredentialDetailViewModel: Decryption successful")
                }
            } else {
                await MainActor.run {
                    self.state.error = "Failed to decode decrypted content"
                }
            }
        } catch {
            if let enclaveError = error.asEnclaveError() {
                await MainActor.run {
                    switch onEnum(of: enclaveError) {
                    case .noKey, .keyExpired:
                        Logger.enclave.notice("CredentialDetailViewModel: No key found, prompting for unlock")
                        self.enclaveUiState = .requiresUnlock(type: orchestrator.getEnclaveType())

                    case .decryptionFailed(let reason):
                        Logger.enclave.error("CredentialDetailViewModel: Decryption failed - \(reason)")
                        let message = reason.description()
                        self.enclaveUiState = .unlockError(
                            type: orchestrator.getEnclaveType(),
                            message: message,
                            canRetry: true
                        )
                        self.state.isDecrypting = false

                    default:
                        Logger.enclave.error("CredentialDetailViewModel: Decryption failed with error: \(enclaveError.message ?? "unknown")")
                        self.state.error = enclaveError.message
                        self.state.isDecrypting = false
                    }
                }
            } else {
                await MainActor.run {
                    self.state.error = "Decryption error: \(error.localizedDescription)"
                    self.state.isDecrypting = false
                }
            }
        }
    }

    // MARK: - Enclave Operations

    private func unlockEnclave(password: String?, config: EnclaveSessionConfig) {
        guard let user = userRepository.getStoredUser() else {
            state.error = "No user ID found"
            return
        }

        Task { @MainActor [weak self] in
            guard let self = self else { return }

            do {
                try await orchestrator.unlock(
                    userId: user.id,
                    sessionConfig: config,
                    password: password
                )
                Logger.enclave.info("CredentialDetailViewModel: Enclave unlocked successfully")
                // State observer will trigger decrypt
            } catch {
                Logger.enclave.error("CredentialDetailViewModel: Unlock failed - \(error.localizedDescription)")
                if let enclaveError = error.asEnclaveError() {
                    self.enclaveUiState = .unlockError(
                        type: orchestrator.getEnclaveType(),
                        message: enclaveError.message ?? "Failed to unlock enclave",
                        canRetry: false
                    )
                } else {
                    self.enclaveUiState = .unlockError(
                        type: orchestrator.getEnclaveType(),
                        message: error.localizedDescription,
                        canRetry: false
                    )
                }
                self.state.isDecrypting = false
            }
        }
    }

    private func lockEnclave() {
        Task { @MainActor in
            do {
                _ = try await orchestrator.lock()
                Logger.enclave.info("CredentialDetailViewModel: Enclave locked successfully")
            } catch {
                // noop
            }
            self.state.decryptedContent = nil
            self.state.error = nil
            self.state.isDecrypting = false
        }
    }

    private func dismissEnclave() {
        enclaveUiState = .hidden
        self.state.isDecrypting = false
    }
}
