import Foundation
import SwiftUI

/// Navigation routes matching Android's NavRoutes.kt
enum Route: Hashable, Identifiable {
    case login
    case mnemonic
    case dashboard
    case credentials
    case credentialDetail(credentialId: String)
    case wallets
    case settings

    var id: String {
        switch self {
        case .login: return "login"
        case .mnemonic: return "mnemonic"
        case .dashboard: return "dashboard"
        case .credentials: return "credentials"
        case .credentialDetail(let id): return "credentialDetail-\(id)"
        case .wallets: return "wallets"
        case .settings: return "settings"
        }
    }
}

/// NavigationCoordinator manages navigation state for iOS 15+
/// This matches Android's NavigationManager pattern using SharedFlow
class NavigationCoordinator: ObservableObject {
    // Array-based navigation stack (iOS 15 compatible)
    @Published var navigationStack: [Route] = []
    @Published var presentedSheet: Route?

    // Computed properties for navigation state
    var currentRoute: Route? {
        navigationStack.last
    }

    func navigate(to route: Route) {
        navigationStack.append(route)
    }

    func navigateUp() {
        if !navigationStack.isEmpty {
            navigationStack.removeLast()
        }
    }

    func popToRoot() {
        navigationStack.removeAll()
    }

    func presentSheet(_ route: Route) {
        presentedSheet = route
    }

    func dismissSheet() {
        presentedSheet = nil
    }

    func replace(with route: Route) {
        navigationStack.removeAll()
        navigationStack.append(route)
    }

    // Helper to check if a specific route is active
    func isRouteActive(_ route: Route) -> Bool {
        return currentRoute == route
    }
}
