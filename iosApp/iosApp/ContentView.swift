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
        .onAppear {
            print("📱 ContentView: onAppear - current state: \(describeUserState(storageManager.userState))")
        }
        .onChange(of: storageManager.userState) { newValue in
            print("🔄 ContentView: State changed to \(describeUserState(newValue))")

            // Log navigation decisions
            switch newValue {
            case .noUser, .connectedWallet:
                print("📱 ContentView: Showing LoginView")
            case .connectedUser:
                print("📱 ContentView: Navigating to DashboardView")
            case .loadingUser:
                print("📱 ContentView: Showing loading state")
            case .userError:
                print("📱 ContentView: Showing error state")
            }
        }
    }

    private func describeUserState(_ state: UserState) -> String {
        switch state {
        case .loadingUser:
            return "loadingUser"
        case .noUser:
            return "noUser"
        case .connectedWallet(let address):
            return "connectedWallet(\(address.prefix(10))...)"
        case .connectedUser(let user):
            return "connectedUser(\(user.id))"
        case .userError(let message):
            return "userError(\(message))"
        }
    }
}
