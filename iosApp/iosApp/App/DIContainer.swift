import Foundation
import idos_sdk
import Combine

// MARK: - App Configuration
struct AppConfig {
    let kwilNodeUrl: String
    let chainId: String
    
#if DEBUG
    static let staging = AppConfig(
        kwilNodeUrl: "https://nodes.staging.idos.network",
        chainId: "idos-staging"
    )
    
    static let preview = AppConfig(
        kwilNodeUrl: "https://nodes.staging.idos.network",
        chainId: "idos-staging"
    )
#else
    static let production = AppConfig(
        kwilNodeUrl: "https://nodes.idos.network",
        chainId: "idos-mainnet"
    )
#endif
}

/// Central Dependency Injection container for the iOS app
/// This follows the architecture pattern from the Android app using Koin
class DIContainer: ObservableObject {
    @MainActor static let shared = DIContainer()

    // MARK: - Security Layer
    let encryption: Encryption
    let metadataStorage: MetadataStorage
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
        // Load appropriate configuration
#if DEBUG
        let config = ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1" 
        ? AppConfig.preview 
        : AppConfig.staging
#else
        let config = AppConfig.production
#endif
        
        // Initialize security components (matching Android's securityModule)
        let secureStorage = IosSecureStorage()
        self.encryption = IosEncryption(storage: secureStorage)
        self.metadataStorage = IosMetadataStorage()
        self.enclaveOrchestrator = EnclaveOrchestrator.companion.create(encryption: encryption,
                                                                        storage: metadataStorage)
        self.keyManager = KeyManager()

        // Initialize data layer (matching Android's repositoryModule)
        self.storageManager = StorageManager()

        // Initialize Ethereum signer (matching Android's EthSigner)
        self.ethSigner = EthSigner(keyManager: keyManager, storageManager: storageManager)
        
        // Initialize network layer with configuration
        self.dataProvider = DataProvider(
            url: config.kwilNodeUrl,
            signer: ethSigner,
            chainId: config.chainId
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
                keyManager: keyManager
            )
            self.walletRepository = WalletRepository(dataProvider: dataProvider)
        }
#else
        self.credentialsRepository = CredentialsRepository(dataProvider: dataProvider)
        self.userRepository = UserRepository(
            dataProvider: dataProvider,
            storageManager: storageManager,
            keyManager: keyManager
        )
        self.walletRepository = WalletRepository(dataProvider: dataProvider)
#endif

        // Initialize navigation (matching Android's navigationModule)
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
            navigationCoordinator: navigationCoordinator
        )
    }

    func makeSettingsViewModel() -> SettingsViewModel {
        SettingsViewModel(
            orchestrator: enclaveOrchestrator,
            keyManager: keyManager,
            navigationCoordinator: navigationCoordinator
        )
    }
}
