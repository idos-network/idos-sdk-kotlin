import Foundation
import idos_sdk

/// Settings state
struct SettingsState {
    var hasEncryptionKey: Bool = false
    var isDeleting: Bool = false
    var error: String? = nil
    var showDeleteConfirmation: Bool = false
}

/// Settings events
enum SettingsEvent {
    case checkKeyStatus
    case deleteEncryptionKey
    case confirmDelete
    case cancelDelete
    case clearError
}

/// SettingsViewModel matching Android's SettingsViewModel
class SettingsViewModel: BaseViewModel<SettingsState, SettingsEvent> {
    private let orchestrator: EnclaveOrchestrator
    private let keyManager: KeyManager
    private let navigationCoordinator: NavigationCoordinator

    init(orchestrator: EnclaveOrchestrator, keyManager: KeyManager, navigationCoordinator: NavigationCoordinator) {
        self.orchestrator = orchestrator
        self.keyManager = keyManager
        self.navigationCoordinator = navigationCoordinator
        super.init(initialState: SettingsState())
        checkKeyStatus()
    }

    override func onEvent(_ event: SettingsEvent) {
        switch event {
        case .checkKeyStatus:
            checkKeyStatus()
        case .deleteEncryptionKey:
            state.showDeleteConfirmation = true
        case .confirmDelete:
            deleteEncryptionKey()
        case .cancelDelete:
            state.showDeleteConfirmation = false
        case .clearError:
            state.error = nil
        }
    }

    private func checkKeyStatus() {
        Task {
            // Check orchestrator state instead of calling hasValidKey directly
            for await flowState in orchestrator.state {
                await MainActor.run {
                    switch flowState {
                    case is EnclaveFlowAvailable, is EnclaveFlowProcessing:
                        state.hasEncryptionKey = true
                    default:
                        state.hasEncryptionKey = false
                    }
                }
                break // Only check once
            }
        }
    }

    private func deleteEncryptionKey() {
        state.isDeleting = true
        state.showDeleteConfirmation = false

        Task {
            do {
                try await orchestrator.resetKey()
                await MainActor.run {
                    state.hasEncryptionKey = false
                    state.isDeleting = false
                }
            } catch {
                await MainActor.run {
                    state.error = "Failed to delete encryption key"
                    state.isDeleting = false
                }
            }
        }
    }
}
