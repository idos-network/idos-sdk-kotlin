import Foundation
import Combine
import idos_sdk

/// Base class for ViewModels that require Enclave operations
/// Extends BaseViewModel and adds Enclave-specific functionality
/// Matches Android's BaseEnclaveViewModel functionality
class BaseEnclaveViewModel<State, Event>: BaseViewModel<State, Event> {
    @Published var enclaveState: EnclaveUiState = .loading
    private let orchestrator: EnclaveOrchestrator
    private var cancellables = Set<AnyCancellable>()

    init(initialState: State, orchestrator: EnclaveOrchestrator) {
        self.orchestrator = orchestrator
        super.init(initialState: initialState)
        observeOrchestratorState()
    }

    /// Observe orchestrator state and map to UI state
    private func observeOrchestratorState() {
        Task { @MainActor in
            // Collect StateFlow from Kotlin
            for await flowState in orchestrator.state {
                enclaveState = mapEnclaveFlowToUiState(flowState)
            }
        }
    }

    /// Map Kotlin EnclaveFlow to Swift EnclaveUiState
    private func mapEnclaveFlowToUiState(_ flow: EnclaveFlow) -> EnclaveUiState {
        switch flow {
        case is EnclaveFlowInitializing:
            return .loading
        case is EnclaveFlowRequiresKey:
            return .requiresKey
        case is EnclaveFlowGeneratingKey:
            return .generating
        case let available as EnclaveFlowAvailable:
            return .available(enclave: available.enclave)
        case let processing as EnclaveFlowProcessing:
            return .available(enclave: processing.enclave)
        case let wrongPassword as EnclaveFlowWrongPasswordSuspected:
            return .keyGenerationError(message: "Decryption failed \(wrongPassword.attemptCount) times. Wrong password?")
        case is EnclaveFlowCancelled:
            return .requiresKey
        case let error as EnclaveFlowError:
            return .error(message: error.message, canRetry: true)
        default:
            return .error(message: "Unknown enclave state", canRetry: true)
        }
    }

    /// Execute an operation that requires a valid Enclave
    /// Uses EnclaveOrchestrator to queue the action
    func requireEnclave(action: @escaping (Enclave) async throws -> Void) {
        print("🔐 BaseEnclaveViewModel: requireEnclave called")
        Task {
            do {
                try await orchestrator.performAction(action: action)
                print("✅ BaseEnclaveViewModel: Action completed successfully")
            } catch {
                print("❌ BaseEnclaveViewModel: Action failed - \(error.localizedDescription)")
            }
        }
    }

    /// Generate a new encryption key using the orchestrator
    func generateKey(userId: String, password: String, expiration: KeyExpiration) {
        print("🔑 BaseEnclaveViewModel: Starting key generation for userId: \(userId), expiration: \(expiration)")
        Task {
            do {
                try await orchestrator.generateKey(
                    userId: userId,
                    password: password,
                    expirationMillis: expiration.rawValue
                )
                print("✅ BaseEnclaveViewModel: Key generation successful")
            } catch {
                print("❌ BaseEnclaveViewModel: Key generation failed - \(error.localizedDescription)")
            }
        }
    }

    /// Retry pending action
    func retryEnclaveCheck() {
        Task {
            do {
                try await orchestrator.retryPendingAction()
            } catch {
                print("❌ BaseEnclaveViewModel: Retry failed - \(error.localizedDescription)")
            }
        }
    }

    /// Cancel pending action and clear error state
    func clearEnclaveError() {
        orchestrator.cancelPendingAction()
    }

    /// Reset/delete the encryption key
    func resetKey() {
        print("🔑 BaseEnclaveViewModel: Resetting encryption key")
        Task {
            do {
                try await orchestrator.resetKey()
                print("✅ BaseEnclaveViewModel: Key reset successful")
            } catch {
                print("❌ BaseEnclaveViewModel: Key reset failed - \(error.localizedDescription)")
            }
        }
    }
}
