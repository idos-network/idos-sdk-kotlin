import Foundation
import Combine

/// Login state matching Android's LoginState
struct LoginState {
    var isLoading: Bool = false
    var error: String? = nil
}

/// Login events matching Android's LoginEvent
enum LoginEvent {
    case connectWallet
    case clearError
}

/// LoginViewModel matching Android's LoginViewModel
class LoginViewModel: BaseViewModel<LoginState, LoginEvent> {
    private let navigationCoordinator: NavigationCoordinator
    private let storageManager: StorageManager

    init(navigationCoordinator: NavigationCoordinator, storageManager: StorageManager) {
        self.navigationCoordinator = navigationCoordinator
        self.storageManager = storageManager
        super.init(initialState: LoginState())
    }

    override func onEvent(_ event: LoginEvent) {
        switch event {
        case .connectWallet:
            connectWallet()
        case .clearError:
            state.error = nil
        }
    }

    private func connectWallet() {
        // Navigate to Mnemonic screen
        navigationCoordinator.navigate(to: .mnemonic)
    }
}