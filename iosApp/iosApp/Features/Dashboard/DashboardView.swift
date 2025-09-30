import SwiftUI

/// DashboardView with drawer navigation matching Android's MainActivity
struct DashboardView: View {
    @EnvironmentObject var diContainer: DIContainer
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

    var body: some View {
        NavigationStack(path: $diContainer.navigationCoordinator.path) {
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
            .navigationDestination(for: Route.self) { route in
                routeDestination(for: route)
            }
        }
    }

    @ViewBuilder
    private func routeDestination(for route: Route) -> some View {
        switch route {
        case .credentialDetail(let credentialId):
            CredentialDetailView(
                viewModel: diContainer.makeCredentialDetailViewModel(credentialId: credentialId)
            )
        default:
            EmptyView()
        }
    }
}