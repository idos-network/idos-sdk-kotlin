import Foundation
import OSLog
import idos_sdk
import Combine

// MARK: - App Configuration
struct AppConfig {
    let kwilNodeUrl: String
    let chainId: String
    let mpcConfig: MpcConfig

    static let playground = AppConfig(
        kwilNodeUrl: "https://nodes.playground.idos.network",
        chainId: "kwil-testnet",
        mpcConfig: MpcConfig(
            partisiaRpcUrl: "https://partisia-reader-node.playground.idos.network:8080",
            contractAddress: "0223996d84146dbf310dd52a0e1d103e91bb8402b3",
            totalNodes: 6,
            threshold: 4,
            maliciousNodes: 2
        )
    )
}

// MARK: - Logging Configuration

extension DIContainer {
    /// SDK logging configuration matching Android's loggingModule
    /// Build-type-aware: Debug = verbose, Release = production-safe
    static func createLogConfig() -> IdosLogConfig {
        #if DEBUG
        // Debug builds: INFO level HTTP logs, DEBUG level SDK logs
        Logger.network.debug("SDK logging configured: HTTP .info, SDK .debug")
        return IdosLogConfig.companion.build { builder in
            builder.httpLogLevel = .info
            builder.sdkLogLevel = .debug
            builder.platformSink()
        }
        #else
        // Release builds: NONE for HTTP logs, INFO level SDK logs
        Logger.network.info("SDK logging configured: HTTP .none, SDK .info")
        return IdosLogConfig.companion.build { builder in
            builder.httpLogLevel = .none
            builder.sdkLogLevel = .info
            builder.platformSink()
        }
        #endif
    }
}

/// Central Dependency Injection container for the iOS app
/// This follows the architecture pattern from the Android app using Koin
class DIContainer: ObservableObject {
    @MainActor static let shared = DIContainer()

    // MARK: - Security Layer
    let encryption: Encryption
    let metadataStorage: MetadataStorage
    let keccak256Hasher: Keccak256Hasher
    let enclaveOrchestrator: EnclaveOrchestrator
    let keyManager: KeyManager
    let ethSigner: EthSigner

    // MARK: - Data Layer
    let storageManager: StorageManager
    let dataProvider: DataProvider

    // Repositories
    let credentialsRepository: CredentialsRepositoryProtocol
    let userRepository: UserRepositoryProtocol
    let walletRepository: WalletRepositoryProtocol

    // MARK: - Navigation
    let navigationCoordinator: NavigationCoordinator

    private init() {
        let config = AppConfig.playground

        let secureStorage = KeychainSecureStorage()  // From SDK via SKIE
        self.encryption = IosEncryption(storage: secureStorage)
        self.metadataStorage = IosMetadataStorage()
        self.keccak256Hasher = Hasher()  // iOS implementation using WalletCore
        self.keyManager = KeyManager()

        self.storageManager = StorageManager()

        self.ethSigner = EthSigner(keyManager: keyManager, storageManager: storageManager, hasher: keccak256Hasher)

        // Initialize EnclaveOrchestrator with MPC support
        self.enclaveOrchestrator = EnclaveOrchestrator.companion.create(
            encryption: encryption,
            storage: metadataStorage,
            mpcConfig: config.mpcConfig,
            signer: ethSigner,
            hasher: keccak256Hasher
        )

        // Initialize network layer with SDK logging configuration
        self.dataProvider = DataProvider(
            url: config.kwilNodeUrl,
            signer: ethSigner,
            chainId: config.chainId,
            logConfig: DIContainer.createLogConfig()
        )

        // Initialize repositories (use mocks for previews)
#if DEBUG
        let isPreview = ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1"
        if isPreview {
            self.credentialsRepository = MockCredentialsRepository()
            self.userRepository = MockUserRepository()
            self.walletRepository = MockWalletRepository()
        } else {
            self.credentialsRepository = CredentialsRepository(dataProvider: dataProvider)
            self.userRepository = UserRepository(
                dataProvider: dataProvider,
                storageManager: storageManager,
                keyManager: keyManager,
                enclaveOrchestrator: enclaveOrchestrator
            )
            self.walletRepository = WalletRepository(dataProvider: dataProvider)
        }
#else
        self.credentialsRepository = CredentialsRepository(dataProvider: dataProvider)
        self.userRepository = UserRepository(
            dataProvider: dataProvider,
            storageManager: storageManager,
            keyManager: keyManager,
            enclaveOrchestrator: enclaveOrchestrator
        )
        self.walletRepository = WalletRepository(dataProvider: dataProvider)
#endif

        self.navigationCoordinator = NavigationCoordinator()
    }

    // MARK: - Factory Methods for ViewModels

    func makeLoginViewModel() -> LoginViewModel {
        LoginViewModel(
            navigationCoordinator: navigationCoordinator,
            storageManager: storageManager
        )
    }

    func makeMnemonicViewModel() -> MnemonicViewModel {
        MnemonicViewModel(
            keyManager: keyManager,
            storageManager: storageManager,
            userRepository: userRepository,
            navigationCoordinator: navigationCoordinator
        )
    }

    func makeCredentialsViewModel() -> CredentialsViewModel {
        CredentialsViewModel(
            credentialsRepository: credentialsRepository,
            navigationCoordinator: navigationCoordinator
        )
    }

    func makeCredentialDetailViewModel(credentialId: String) -> CredentialDetailViewModel {
        CredentialDetailViewModel(
            credentialId: credentialId,
            orchestrator: enclaveOrchestrator,
            credentialsRepository: credentialsRepository,
            userRepository: userRepository,
            navigationCoordinator: navigationCoordinator
        )
    }

    func makeWalletsViewModel() -> WalletsViewModel {
        WalletsViewModel(
            walletRepository: walletRepository,
            navigationCoordinator: navigationCoordinator
        )
    }

    func makeSettingsViewModel() -> SettingsViewModel {
        SettingsViewModel(
            orchestrator: enclaveOrchestrator,
            metadataStorage: metadataStorage,
            keyManager: keyManager,
            navigationCoordinator: navigationCoordinator
        )
    }
}
