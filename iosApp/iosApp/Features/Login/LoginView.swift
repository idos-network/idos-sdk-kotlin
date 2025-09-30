import SwiftUI

/// LoginView matching Android's LoginScreen
struct LoginView: View {
    @StateObject var viewModel: LoginViewModel
    @EnvironmentObject var diContainer: DIContainer

    var body: some View {
        NavigationStack(path: $diContainer.navigationCoordinator.path) {
            VStack(spacing: 32) {
                Spacer()

                // App Icon
                Image(systemName: "shield.checkered")
                    .font(.system(size: 80))
                    .foregroundColor(.blue)

                // Title
                Text("Welcome")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text("Secure identity and data management")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)

                Spacer()

                // Connect Wallet Button
                Button(action: {
                    viewModel.onEvent(.connectWallet)
                }) {
                    Text("Connect Wallet")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 32)

                Spacer()
                    .frame(height: 60)
            }
            .navigationDestination(for: Route.self) { route in
                routeDestination(for: route)
            }
        }
        .environmentObject(diContainer.navigationCoordinator)
    }

    @ViewBuilder
    private func routeDestination(for route: Route) -> some View {
        switch route {
        case .mnemonic:
            MnemonicView()
        case .dashboard:
            DashboardView()
        default:
            EmptyView()
        }
    }
}