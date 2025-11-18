import Foundation
import Combine
import OSLog
import ReownAppKit

/// Login state matching Android's LoginState
struct LoginState {
    var isLoading: Bool = false
    var error: String? = nil
}

/// Login events matching Android's LoginEvent
enum LoginEvent {
    case importMnemonic
    case connectExternalWallet
    case walletConnected
    case clearError
}

/// LoginViewModel matching Android's LoginViewModel
class LoginViewModel: BaseViewModel<LoginState, LoginEvent> {
    private let navigationCoordinator: NavigationCoordinator
    private let unifiedSigner: UnifiedSigner
    private let userRepository: UserRepositoryProtocol
    private let reownWalletManager: ReownWalletManager

    private var cancellables = Set<AnyCancellable>()

    init(
        navigationCoordinator: NavigationCoordinator,
        unifiedSigner: UnifiedSigner,
        userRepository: UserRepositoryProtocol,
        reownWalletManager: ReownWalletManager
    ) {
        self.navigationCoordinator = navigationCoordinator
        self.unifiedSigner = unifiedSigner
        self.userRepository = userRepository
        self.reownWalletManager = reownWalletManager
        super.init(initialState: LoginState())

        // Monitor wallet connection events
        monitorWalletConnection()
    }

    override func onEvent(_ event: LoginEvent) {
        switch event {
        case .importMnemonic:
            importMnemonic()
        case .connectExternalWallet:
            connectExternalWallet()
        case .walletConnected:
            handleWalletConnected()
        case .clearError:
            updateState { $0.error = nil }
        }
    }

    private func importMnemonic() {
        // Navigate to Mnemonic screen
        navigationCoordinator.navigate(to: .mnemonic)
    }

    private func connectExternalWallet() {
        // Present AppKit modal
        Task { @MainActor in
            AppKit.present()
        }
    }

    private func monitorWalletConnection() {
        ReownDelegate.shared.sessionApproved
            .sink { [weak self] topic in
                Logger.viewModel.debug("LoginViewModel: Session approved - \(topic)")
                self?.onEvent(.walletConnected)
            }
            .store(in: &cancellables)
    }

    private func handleWalletConnected() {
        Logger.viewModel.debug("LoginViewModel: Wallet connected, activating remote signer")

        updateState {
            $0.isLoading = true
        }

        Task {
            do {
                // Activate remote signer
                unifiedSigner.activateRemoteSigner()
                Logger.viewModel.info("LoginViewModel: Remote signer activated")

                // Fetch and store user
                try await userRepository.fetchAndStoreUser(walletType: .remote)
                Logger.viewModel.info("LoginViewModel: User fetched and stored successfully")

                await MainActor.run {
                    updateState { $0.isLoading = false }
                }
            } catch {
                Logger.viewModel.error("LoginViewModel: Failed to fetch user: \(error.localizedDescription)")
                await MainActor.run {
                    updateState {
                        $0.isLoading = false
                        $0.error = "Failed to fetch user: \(error.localizedDescription)"
                    }
                }
            }
        }
    }
}