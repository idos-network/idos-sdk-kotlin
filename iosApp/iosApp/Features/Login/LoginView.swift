import SwiftUI
import ReownAppKit

/// LoginView matching Android's LoginScreen
struct LoginView: View {
    @StateObject var viewModel: LoginViewModel
    @EnvironmentObject var diContainer: DIContainer
    @EnvironmentObject var navigationCoordinator: NavigationCoordinator

    // iOS 15 compatible navigation state
    private var isMnemonicActive: Binding<Bool> {
        Binding(
            get: { navigationCoordinator.currentRoute == .mnemonic },
            set: { if !$0 { navigationCoordinator.navigateUp() } }
        )
    }

    private var isDashboardActive: Binding<Bool> {
        Binding(
            get: { navigationCoordinator.currentRoute == .dashboard },
            set: { if !$0 { navigationCoordinator.navigateUp() } }
        )
    }

    var body: some View {
        NavigationView {
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

                // Import Mnemonic Button
                Button(action: {
                    viewModel.onEvent(.importMnemonic)
                }) {
                    Text("Import Wallet")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 32)

                // Connect External Wallet Button
                Button(action: {
                    viewModel.onEvent(.connectExternalWallet)
                }) {
                    Text("Connect Wallet")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 32)

                if viewModel.state.isLoading {
                    ProgressView()
                        .padding()
                }

                if let error = viewModel.state.error {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }

                Spacer()
                    .frame(height: 60)

                // Hidden NavigationLinks for programmatic navigation (iOS 15 compatible)
                NavigationLink(
                    destination: MnemonicView(viewModel: diContainer.makeMnemonicViewModel()),
                    isActive: isMnemonicActive
                ) { EmptyView() }

                NavigationLink(
                    destination: DashboardView(),
                    isActive: isDashboardActive
                ) { EmptyView() }
            }
            .navigationBarHidden(true)
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

#Preview {
    let diContainer = DIContainer.shared
    LoginView(viewModel: diContainer.makeLoginViewModel())
        .environmentObject(diContainer)
        .environmentObject(diContainer.navigationCoordinator)
}
