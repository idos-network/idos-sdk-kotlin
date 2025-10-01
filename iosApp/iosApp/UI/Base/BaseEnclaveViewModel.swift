import Foundation
import Combine
import idos_sdk

/// Base class for ViewModels that require Enclave operations
/// Extends BaseViewModel and adds Enclave-specific functionality
/// Matches Android's BaseEnclaveViewModel functionality
class BaseEnclaveViewModel<State, Event>: BaseViewModel<State, Event> {
    @Published var enclaveState: EnclaveUiState = .loading
    private let enclave: Enclave
    private var cancellables = Set<AnyCancellable>()
    private var pendingAction: ((Enclave) async throws -> Void)?

    init(initialState: State, enclave: Enclave) {
        self.enclave = enclave
        super.init(initialState: initialState)
        checkEnclaveStatus()
    }

    /// Check if Enclave has a valid key
    private func checkEnclaveStatus() {
        print("🔐 BaseEnclaveViewModel: Starting enclave status check")
        Task { @MainActor in
            do {
                enclaveState = .loading
                print("🔐 BaseEnclaveViewModel: Enclave state = loading")
                try await enclave.hasValidKey()
                enclaveState = .available(enclave: enclave)
                print("✅ BaseEnclaveViewModel: Enclave has valid key, state = available")
            } catch {
                print("🔍 BaseEnclaveViewModel: Caught error type: \(type(of: error))")
                print("🔍 BaseEnclaveViewModel: Error description: \(error.localizedDescription)")

                // Check if it's a key-related error by message
                let errorMessage = error.localizedDescription
                if errorMessage.contains("No key present") || errorMessage.contains("Key expired") ||
                   error is KeyExpiredError || error is NoKeyError {
                    enclaveState = .requiresKey
                    print("⚠️ BaseEnclaveViewModel: No valid key found, state = requiresKey")
                } else {
                    enclaveState = .error(message: errorMessage, canRetry: true)
                    print("❌ BaseEnclaveViewModel: Error checking enclave - \(errorMessage)")
                }
            }
        }
    }

    /// Execute an operation that requires a valid Enclave
    /// Matches Android's requireEnclave() pattern
    func requireEnclave(action: @escaping (Enclave) async throws -> Void) {
        print("🔐 BaseEnclaveViewModel: requireEnclave called, current state = \(enclaveState)")
        Task { @MainActor in
            guard case .available(let enclave) = enclaveState else {
                print("⚠️ BaseEnclaveViewModel: Enclave not available, storing action for later")
                pendingAction = action
                enclaveState = .requiresKey
                return
            }

            print("🔐 BaseEnclaveViewModel: Executing action with available enclave")
            do {
                try await action(enclave)
                print("✅ BaseEnclaveViewModel: Action completed successfully")
                pendingAction = nil
            } catch {
                print("🔍 BaseEnclaveViewModel: Caught error type: \(type(of: error))")
                print("🔍 BaseEnclaveViewModel: Error description: \(error.localizedDescription)")

                // Check if it's a key-related error by message or type
                let errorMessage = error.localizedDescription
                if errorMessage.contains("No key present") || errorMessage.contains("Key expired") ||
                   error is KeyExpiredError || error is NoKeyError {
                    print("⚠️ BaseEnclaveViewModel: Key expired or not found, state = requiresKey")
                    pendingAction = action
                    enclaveState = .requiresKey
                } else {
                    print("❌ BaseEnclaveViewModel: Action failed - \(errorMessage)")
                    pendingAction = nil
                    enclaveState = .error(message: errorMessage, canRetry: true)
                }
            }
        }
    }

    /// Generate a new encryption key
    /// Matches Android's KeyGenerationDialog flow
    func generateKey(userId: String, password: String, expiration: KeyExpiration) {
        print("🔑 BaseEnclaveViewModel: Starting key generation for userId: \(userId), expiration: \(expiration)")
        Task { @MainActor in
            do {
                enclaveState = .generating
                print("🔑 BaseEnclaveViewModel: Enclave state = generating")
                try await enclave.generateKey(
                    userId: userId,
                    password: password,
                    expiration: expiration.rawValue
                )
                enclaveState = .available(enclave: enclave)
                print("✅ BaseEnclaveViewModel: Key generation successful, state = available")

                // Execute pending action if one exists
                if let action = pendingAction {
                    print("🔄 BaseEnclaveViewModel: Executing pending action after key generation")
                    pendingAction = nil
                    do {
                        try await action(enclave)
                        print("✅ BaseEnclaveViewModel: Pending action completed successfully")
                    } catch {
                        print("❌ BaseEnclaveViewModel: Pending action failed - \(error.localizedDescription)")
                        enclaveState = .error(message: error.localizedDescription, canRetry: true)
                    }
                }
            } catch {
                print("❌ BaseEnclaveViewModel: Key generation failed - \(error.localizedDescription)")
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

    /// Reset/delete the encryption key
    func resetKey() {
        print("🔑 BaseEnclaveViewModel: Resetting encryption key")
        Task { @MainActor in
            do {
                pendingAction = nil
                try await enclave.deleteKey()
                enclaveState = .requiresKey
                print("✅ BaseEnclaveViewModel: Key reset successful, state = requiresKey")
            } catch {
                print("❌ BaseEnclaveViewModel: Key reset failed - \(error.localizedDescription)")
                enclaveState = .error(message: error.localizedDescription, canRetry: false)
            }
        }
    }
}
