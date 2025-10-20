import OSLog

extension Logger {
    /// App subsystem (bundle identifier)
    private static let subsystem = Bundle.main.bundleIdentifier ?? "org.idos.app"

    // MARK: - Logger Categories

    /// ViewModel operations and state changes
    static let viewModel = Logger(subsystem: subsystem, category: "ViewModel")

    /// Network requests and API calls
    static let network = Logger(subsystem: subsystem, category: "Network")

    /// Storage operations (UserDefaults, Keychain)
    static let storage = Logger(subsystem: subsystem, category: "Storage")

    /// Encryption and enclave operations
    static let enclave = Logger(subsystem: subsystem, category: "Enclave")

    /// Security operations (key management, signing)
    static let security = Logger(subsystem: subsystem, category: "Security")

    /// Navigation and routing
    static let navigation = Logger(subsystem: subsystem, category: "Navigation")

    /// Repository layer operations
    static let repository = Logger(subsystem: subsystem, category: "Repository")
}

// MARK: - Log Level Guidelines
/*
 Use log levels appropriately:

 - .debug: Development-only verbose info (filtered out in release)
   Example: "CredentialDetailViewModel: Initializing for credentialId: \(id)"

 - .info: General informational events
   Example: "User profile loaded successfully"

 - .notice: Important but not error conditions
   Example: "Enclave initialized with type: MPC"

 - .error: Error conditions that need attention
   Example: "Failed to load credential: \(error.localizedDescription)"

 - .fault: Critical errors requiring immediate attention
   Example: "Fatal: Cannot create API client"
*/
