import SwiftUI
import idos_sdk

@main
struct iosApp: App {

    init() {
        // Initialize KMM shared module
        // This ensures the shared module is loaded

        // Initialize user repository to load stored enclave type
        Task { @MainActor in
            DIContainer.shared.userRepository.initialize()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(DIContainer.shared)
                .environmentObject(DIContainer.shared.navigationCoordinator)
        }
    }
}