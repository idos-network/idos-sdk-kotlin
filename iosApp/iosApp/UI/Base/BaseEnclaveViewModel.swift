import Foundation
import Combine
import idos_sdk

/// Base class for ViewModels that require Enclave operations
/// Matches Android's BaseEnclaveViewModel functionality
class BaseEnclaveViewModel<State, Event>: ObservableObject {
    @Published var enclaveState: EnclaveUiState = .loading
    private let enclave: Enclave
    private var cancellables = Set<AnyCancellable>()

    init(enclave: Enclave) {
        self.enclave = enclave
        checkEnclaveStatus()
    }

    /// Check if Enclave has a valid key
    private func checkEnclaveStatus() {
        Task { @MainActor in
            do {
                enclaveState = .loading
                let hasKey = try await enclave.hasValidKey()
                if hasKey {
                    enclaveState = .available(enclave: enclave)
                } else {
                    enclaveState = .requiresKey
                }
            } catch let error as EnclaveError.KeyExpiredError {
                enclaveState = .requiresKey
            } catch {
                enclaveState = .error(message: error.localizedDescription, canRetry: true)
            }
        }
    }

    /// Execute an operation that requires a valid Enclave
    /// Matches Android's requireEnclave() pattern
    func requireEnclave(action: @escaping (Enclave) async throws -> Void) {
        Task { @MainActor in
            guard case .available(let enclave) = enclaveState else {
                enclaveState = .requiresKey
                return
            }

            do {
                try await action(enclave)
            } catch let error as EnclaveError.KeyExpiredError {
                enclaveState = .requiresKey
            } catch let error as EnclaveError.NoKeyError {
                enclaveState = .requiresKey
            } catch {
                enclaveState = .error(message: error.localizedDescription, canRetry: true)
            }
        }
    }

    /// Generate a new encryption key
    /// Matches Android's KeyGenerationDialog flow
    func generateKey(userId: String, password: String, expiration: KeyExpiration) {
        Task { @MainActor in
            do {
                enclaveState = .generating
                try await enclave.generateKey(
                    userId: userId,
                    password: password,
                    expiration: expiration.rawValue
                )
                enclaveState = .available(enclave: enclave)
            } catch {
                enclaveState = .keyGenerationError(message: error.localizedDescription)
            }
        }
    }

    /// Retry checking Enclave status
    func retryEnclaveCheck() {
        checkEnclaveStatus()
    }

    /// Clear error state and return to requiresKey
    func clearEnclaveError() {
        enclaveState = .requiresKey
    }
}