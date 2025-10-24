import SwiftUI
import OSLog
import idos_sdk

@main
struct iosApp: App {

    init() {
        // Handle UI test reset
        if ProcessInfo.processInfo.arguments.contains("RESET_STATE") {
            resetAppState()
        }

        // Configure logging
        configureLogging()

        // Initialize KMM shared module
        // This ensures the shared module is loaded

        // Initialize user repository to load stored enclave type
        Task { @MainActor in
            DIContainer.shared.userRepository.initialize()
        }
    }

    /// Reset app state for UI testing
    private func resetAppState() {
        // Clear UserDefaults
        if let bundleID = Bundle.main.bundleIdentifier {
            UserDefaults.standard.removePersistentDomain(forName: bundleID)
        }

        // Clear Keychain
        let secItemClasses = [
            kSecClassGenericPassword,
            kSecClassInternetPassword,
            kSecClassCertificate,
            kSecClassKey,
            kSecClassIdentity
        ]
        for secItemClass in secItemClasses {
            let dictionary = [kSecClass as String: secItemClass]
            SecItemDelete(dictionary as CFDictionary)
        }

        // Clear app files
        if let appSupport = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first {
            try? FileManager.default.removeItem(at: appSupport)
        }
        if let cache = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first {
            try? FileManager.default.removeItem(at: cache)
        }
    }

    /// Configure application logging
    ///
    /// App-level logging uses OSLog with categories defined in Utils/AppLogger.swift
    /// SDK logging is configured in DIContainer.createLogConfig() with build-specific levels
    private func configureLogging() {
        #if DEBUG
        // In debug builds, all log levels are visible in Xcode Console
        Logger.viewModel.debug("Logger initialized - Debug build")
        Logger.viewModel.info("idOS iOS App starting...")
        #else
        // In release builds, only .info and above are visible
        Logger.viewModel.info("idOS iOS App starting (Release)")
        #endif
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(DIContainer.shared)
                .environmentObject(DIContainer.shared.navigationCoordinator)
        }
    }
}