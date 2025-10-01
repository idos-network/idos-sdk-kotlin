import SwiftUI

/// DashboardView with drawer navigation matching Android's MainActivity
struct DashboardView: View {
    @EnvironmentObject var diContainer: DIContainer
    @EnvironmentObject var navigationCoordinator: NavigationCoordinator
    @State private var selectedTab: DrawerItem = .credentials

    enum DrawerItem: String, CaseIterable {
        case credentials = "Credentials"
        case wallets = "Wallets"
        case settings = "Settings"

        var icon: String {
            switch self {
            case .credentials: return "doc.text"
            case .wallets: return "wallet.pass"
            case .settings: return "gearshape"
            }
        }
    }

    // Extract credential ID from current route if it's a credentialDetail
    private var currentCredentialId: String? {
        if case .credentialDetail(let id) = navigationCoordinator.currentRoute {
            return id
        }
        return nil
    }

    // iOS 15 compatible navigation state for credential detail
    private var isCredentialDetailActive: Binding<Bool> {
        Binding(
            get: {
                if case .credentialDetail = navigationCoordinator.currentRoute {
                    return true
                }
                return false
            },
            set: { if !$0 { navigationCoordinator.navigateUp() } }
        )
    }

    var body: some View {
        NavigationView {
            ZStack {
                TabView(selection: $selectedTab) {
                    CredentialsView(
                        viewModel: diContainer.makeCredentialsViewModel()
                    )
                    .tabItem {
                        Label(DrawerItem.credentials.rawValue, systemImage: DrawerItem.credentials.icon)
                    }
                    .tag(DrawerItem.credentials)

                    WalletsView(
                        viewModel: diContainer.makeWalletsViewModel()
                    )
                    .tabItem {
                        Label(DrawerItem.wallets.rawValue, systemImage: DrawerItem.wallets.icon)
                    }
                    .tag(DrawerItem.wallets)

                    SettingsView(
                        viewModel: diContainer.makeSettingsViewModel()
                    )
                    .tabItem {
                        Label(DrawerItem.settings.rawValue, systemImage: DrawerItem.settings.icon)
                    }
                    .tag(DrawerItem.settings)
                }

                // Hidden NavigationLink for programmatic navigation (iOS 15 compatible)
                if let credentialId = currentCredentialId {
                    NavigationLink(
                        destination: CredentialDetailView(
                            viewModel: diContainer.makeCredentialDetailViewModel(credentialId: credentialId)
                        ),
                        isActive: isCredentialDetailActive
                    ) { EmptyView() }
                }
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

#Preview {
    let diContainer = DIContainer.shared
    DashboardView()
        .environmentObject(diContainer)
        .environmentObject(diContainer.navigationCoordinator)
}