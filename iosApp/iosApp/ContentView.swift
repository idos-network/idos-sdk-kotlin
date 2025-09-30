import SwiftUI

/// Main ContentView that routes to Login or Dashboard based on user state
/// Matches Android's flow: LoginActivity → MainActivity based on profile
struct ContentView: View {
    @EnvironmentObject var diContainer: DIContainer
    @ObservedObject var storageManager: StorageManager

    init() {
        self.storageManager = DIContainer.shared.storageManager
    }

    var body: some View {
        Group {
            switch storageManager.userState {
            case .loadingUser:
                LoadingStateView(message: "Loading...")

            case .noUser, .connectedWallet:
                LoginView(viewModel: diContainer.makeLoginViewModel())

            case .connectedUser:
                DashboardView()

            case .userError(let message):
                ErrorStateView(
                    message: message,
                    canRetry: true,
                    onRetry: nil
                )
            }
        }
    }
}