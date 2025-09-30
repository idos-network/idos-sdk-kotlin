import SwiftUI
import idos_sdk

@main
struct iosApp: App {

    init() {
        // Initialize KMM shared module
        // This ensures the shared module is loaded
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(DIContainer.shared)
        }
    }
}