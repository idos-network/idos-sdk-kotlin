import Foundation
import SwiftUI

/// Navigation routes matching Android's NavRoutes.kt
enum Route: Hashable {
    case login
    case mnemonic
    case dashboard
    case credentials
    case credentialDetail(credentialId: String)
    case wallets
    case settings
}

/// NavigationCoordinator manages navigation state
/// This matches Android's NavigationManager pattern using SharedFlow
class NavigationCoordinator: ObservableObject {
    @Published var path: NavigationPath = NavigationPath()
    @Published var presentedSheet: Route?

    func navigate(to route: Route) {
        path.append(route)
    }

    func navigateUp() {
        if !path.isEmpty {
            path.removeLast()
        }
    }

    func popToRoot() {
        path = NavigationPath()
    }

    func presentSheet(_ route: Route) {
        presentedSheet = route
    }

    func dismissSheet() {
        presentedSheet = nil
    }

    func replace(with route: Route) {
        path = NavigationPath()
        path.append(route)
    }
}