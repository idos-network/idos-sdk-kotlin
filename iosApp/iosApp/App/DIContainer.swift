import Foundation
import idos_sdk

/// Central Dependency Injection container for the iOS app
/// This follows the architecture pattern from the Android app using Koin
class DIContainer: ObservableObject {
    static let shared = DIContainer()

    // MARK: - Security Layer
    let encryption: Encryption
    let metadataStorage: MetadataStorage
    let enclave: Enclave
    let keyManager: KeyManager

    // MARK: - Data Layer
    let storageManager: StorageManager

    // MARK: - Navigation
    let navigationCoordinator: NavigationCoordinator

    private init() {
        // Initialize security components (matching Android's securityModule)
        self.encryption = IosEncryption()
        self.metadataStorage = IosMetadataStorage()
        self.enclave = Enclave(
            encryption: encryption,
            storage: metadataStorage
        )
        self.keyManager = KeyManager()

        // Initialize data layer (matching Android's repositoryModule)
        self.storageManager = StorageManager()

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

    func makeCredentialsViewModel() -> CredentialsViewModel {
        CredentialsViewModel(
            enclave: enclave,
            navigationCoordinator: navigationCoordinator
        )
    }

    func makeCredentialDetailViewModel(credentialId: String) -> CredentialDetailViewModel {
        CredentialDetailViewModel(
            credentialId: credentialId,
            enclave: enclave,
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
            enclave: enclave,
            keyManager: keyManager,
            navigationCoordinator: navigationCoordinator
        )
    }
}