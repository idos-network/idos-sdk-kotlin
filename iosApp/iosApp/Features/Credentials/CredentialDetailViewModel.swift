import Foundation
import UIKit
import idos_sdk
import Combine

// MARK: - Enclave UI State

/// Enclave dialog/UI state (matches Android EnclaveUiState)
enum EnclaveUiState {
    case hidden
    case requiresUnlock
    case unlocking
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
    case unlockEnclave(password: String, expiration: KeyExpiration)
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
        print("📄 CredentialDetailViewModel: Initializing for credentialId: \(credentialId)")
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
        print("📄 CredentialDetailViewModel: on event: \(event)")
        switch event {
        case .loadCredential:
            loadCredential()
        case .decryptCredential:
            triggerDecrypt()
        case .clearError:
            state.error = nil
        case .unlockEnclave(let password, let expiration):
            unlockEnclave(password: password, expiration: expiration)
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
                print("📄 CredentialDetailViewModel: Starting observeEnclaveState: \(enclaveState)")
                switch enclaveState {
                case is EnclaveState.Locked:
                    // Don't automatically show dialog
                    break

                case is EnclaveState.Unlocking:
                    self.enclaveUiState = .unlocking

                case let unlocked as EnclaveState.Unlocked:
                    self.enclaveUiState = .hidden
                    // Auto-decrypt when unlocked
                    guard let credential = state.credential else { return }
                    await decryptLoadedCredential(enclave: unlocked.enclave, credential: credential)

                default:
                    break
                }
            }
        }
    }

    // MARK: - Load Credential

    private func loadCredential() {
        print("📄 CredentialDetailViewModel: Starting loadCredential for id: \(credentialId)")
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
                print("❌ CredentialDetailViewModel: Load failed - \(error.localizedDescription)")
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
        } else {
            // Show unlock dialog
            enclaveUiState = .requiresUnlock
        }
    }

    private func decryptLoadedCredential(enclave: Enclave, credential: CredentialDetail) async {
        guard state.decryptedContent == nil else { return } // Already decrypted

        print("🔐 CredentialDetailViewModel: Starting decryption")

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
                    print("✅ CredentialDetailViewModel: Decryption successful")
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
                        print("🔑 CredentialDetailViewModel: No key found")
                        self.enclaveUiState = .requiresUnlock
                        
                    case .decryptionFailed(let reason):
                        print("❌ CredentialDetailViewModel: Decryption failed - \(reason)")
                        self.state.error = reason.description()
                        
                    default:
                        print("❌ CredentialDetailViewModel: Decryption failed")
                        self.state.error = enclaveError.message
                    }
                }
            } else {
                await MainActor.run {
                    self.state.error = "Decryption error: \(error.localizedDescription)"
                }
            }
        }
    }

    // MARK: - Enclave Operations

    private func unlockEnclave(password: String, expiration: KeyExpiration) {
        guard let user = userRepository.getStoredUser() else {
            state.error = "No user ID found"
            return
        }

        Task { @MainActor [weak self] in
            guard let self = self else { return }
            
            do {
                try await orchestrator.unlock(
                    userId: user.id,
                    password: password,
                    expirationMillis: expiration.rawValue
                )
                print("✅ CredentialDetailViewModel: Enclave unlocked successfully")
                // State observer will trigger decrypt
            } catch {
                print("❌ CredentialDetailViewModel: Unlock failed - \(error.localizedDescription)")
                self.enclaveUiState = .hidden
                self.state.error = error.localizedDescription
            }
        }
    }

    private func lockEnclave() {
        Task { @MainActor in
            do {
                _ = try await orchestrator.lock()
                print("✅ CredentialDetailViewModel: Enclave lock successful")
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
