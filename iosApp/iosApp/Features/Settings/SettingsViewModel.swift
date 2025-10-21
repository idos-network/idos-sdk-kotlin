import Foundation
import OSLog
import idos_sdk

/// Settings state
struct SettingsState {
    var hasEncryptionKey: Bool = false
    var enclaveStatus: EnclaveUiStatus = .notAvailable
    var isDeleting: Bool = false
    var error: String? = nil
    var showDeleteConfirmation: Bool = false
    var snackbarMessage: String? = nil
}

/// Enclave UI status for display
enum EnclaveUiStatus {
    case unlocked(metadata: KeyMetadata, formattedExpiration: String)
    case locked(metadata: KeyMetadata?, formattedExpiration: String?)
    case notAvailable
    case unlocking
}

/// Settings events
enum SettingsEvent {
    case checkKeyStatus
    case onEncryptionStatusClick
    case deleteEncryptionKey
    case confirmDelete
    case cancelDelete
    case clearError
    case dismissSnackbar
    case disconnectWallet
}

/// SettingsViewModel matching Android's SettingsViewModel
class SettingsViewModel: BaseViewModel<SettingsState, SettingsEvent> {
    private let orchestrator: EnclaveOrchestrator
    private let metadataStorage: MetadataStorage
    private let keyManager: KeyManager
    private let navigationCoordinator: NavigationCoordinator
    private let userRepository: UserRepositoryProtocol

    init(
        orchestrator: EnclaveOrchestrator,
        metadataStorage: MetadataStorage,
        keyManager: KeyManager,
        navigationCoordinator: NavigationCoordinator,
        userRepository: UserRepositoryProtocol
    ) {
        self.orchestrator = orchestrator
        self.metadataStorage = metadataStorage
        self.keyManager = keyManager
        self.navigationCoordinator = navigationCoordinator
        self.userRepository = userRepository
        super.init(initialState: SettingsState())
        observeEnclaveState()
    }

    override func onEvent(_ event: SettingsEvent) {
        switch event {
        case .checkKeyStatus:
            checkKeyStatus()
        case .onEncryptionStatusClick:
            onEncryptionStatusClick()
        case .deleteEncryptionKey:
            state.showDeleteConfirmation = true
        case .confirmDelete:
            deleteEncryptionKey()
        case .cancelDelete:
            state.showDeleteConfirmation = false
        case .clearError:
            state.error = nil
        case .dismissSnackbar:
            state.snackbarMessage = nil
        case .disconnectWallet:
            disconnectWallet()
        }
    }

    private func observeEnclaveState() {
        Task { @MainActor in
            for await enclaveState in orchestrator.state {
                switch enclaveState {
                case is EnclaveState.Unlocked:
                    if let metadata = await fetchMetadata() {
                        let status = EnclaveUiStatus.unlocked(
                            metadata: metadata,
                            formattedExpiration: formatExpiration(metadata)
                        )
                        state.hasEncryptionKey = true
                        state.enclaveStatus = status
                    } else {
                        state.enclaveStatus = .notAvailable
                    }

                case is EnclaveState.Unlocking:
                    state.enclaveStatus = .unlocking

                case is EnclaveState.Locked:
                    let metadata = await fetchMetadata()
                    let status = EnclaveUiStatus.locked(
                        metadata: metadata,
                        formattedExpiration: metadata.map { formatExpiration($0) }
                    )
                    state.hasEncryptionKey = false
                    state.enclaveStatus = status

                case is EnclaveState.NotAvailable:
                    state.hasEncryptionKey = false
                    state.enclaveStatus = .notAvailable

                default:
                    break
                }
            }
        }
    }

    private func fetchMetadata() async -> KeyMetadata? {
        do {
            // Try USER type first, then MPC
            if let metadata = try await metadataStorage.get(enclaveKeyType: .user) {
                return metadata
            }
            if let metadata = try await metadataStorage.get(enclaveKeyType: .mpc) {
                return metadata
            }
            return nil
        } catch {
            Logger.viewModel.error("SettingsViewModel: Failed to fetch metadata - \(error)")
            return nil
        }
    }

    private func formatExpiration(_ metadata: KeyMetadata) -> String {
        if let expiresAt = metadata.expiresAt {
            let date = Date(timeIntervalSince1970: TimeInterval(truncating: expiresAt) / 1000.0)
            let formatter = DateFormatter()
            formatter.dateFormat = "MMM dd, yyyy HH:mm"
            return "Expires: \(formatter.string(from: date))"
        }

        switch metadata.expirationType {
        case .session:
            return "Expires: End of session"
        case .oneShot:
            return "Expires: After first use"
        case .timed:
            return "No expiration"
        @unknown default:
            return "No expiration"
        }
    }

    private func onEncryptionStatusClick() {
        let message: String
        switch state.enclaveStatus {
        case .unlocked(let metadata, _):
            message = buildMetadataMessage(metadata: metadata, statusLabel: "UNLOCKED")
        case .locked(let metadata, _):
            if let metadata = metadata {
                message = buildMetadataMessage(metadata: metadata, statusLabel: "LOCKED")
            } else {
                message = "Enclave is locked\nNo metadata available (key may have expired)"
            }
        case .notAvailable:
            message = "Enclave not available\nNo encryption key has been generated"
        case .unlocking:
            message = "Enclave is unlocking..."
        }
        state.snackbarMessage = message
    }

    private func buildMetadataMessage(metadata: KeyMetadata, statusLabel: String) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM dd, yyyy HH:mm"

        let createdDate = Date(timeIntervalSince1970: TimeInterval(metadata.createdAt) / 1000.0)
        let lastUsedDate = Date(timeIntervalSince1970: TimeInterval(metadata.lastUsedAt) / 1000.0)

        return """
        Status: \(statusLabel)
        Type: \(metadata.type)
        Expiration Type: \(metadata.expirationType)
        \(formatExpiration(metadata))
        Created: \(formatter.string(from: createdDate))
        Last Used: \(formatter.string(from: lastUsedDate))
        Public Key: \(String(metadata.publicKey.prefix(16)))...
        """
    }

    private func checkKeyStatus() {
        Task { @MainActor in
            try? await orchestrator.checkStatus()
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
                Logger.viewModel.info("SettingsViewModel: Encryption key deleted successfully")
            } catch {
                state.error = error.localizedDescription
                state.isDeleting = false
                Logger.viewModel.error("SettingsViewModel: Delete failed - \(error.localizedDescription)")
            }
        }
    }

    private func disconnectWallet() {
        Task { @MainActor in
            Logger.viewModel.info("SettingsViewModel: Disconnecting wallet")
            await userRepository.clearUserProfile()
            navigationCoordinator.replace(with: .login)
        }
    }
}
