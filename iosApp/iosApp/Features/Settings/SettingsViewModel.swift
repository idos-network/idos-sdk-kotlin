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
        Task { @MainActor in
            do {
                try await orchestrator.checkStatus()
            } catch {
            }

            // Check current state value
            let currentState = orchestrator.state.value
            switch currentState {
            case is EnclaveState.Unlocked, is EnclaveState.Unlocking:
                state.hasEncryptionKey = true
            case is EnclaveState.Locked:
                state.hasEncryptionKey = false
            default:
                state.hasEncryptionKey = false
            }
        }
    }

    private func deleteEncryptionKey() {
        state.isDeleting = true
        state.showDeleteConfirmation = false

        Task { @MainActor in
            do {
                try await orchestrator.lock()
                state.hasEncryptionKey = false
                state.isDeleting = false
                print("✅ SettingsViewModel: Encryption key deleted successfully")
            } catch {
                state.error = error.localizedDescription
                state.isDeleting = false
                print("❌ SettingsViewModel: Delete failed - \(error.localizedDescription)")
            }
        }
    }
}
