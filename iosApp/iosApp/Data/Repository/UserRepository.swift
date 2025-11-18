import Combine
import Foundation
import OSLog
import idos_sdk

enum UserError: Error, LocalizedError {
    case noKeyFound
    case noUserProfile
    case networkError(Error)

    var errorDescription: String? {
        switch self {
        case .noKeyFound:
            return "No wallet key found. Please import a wallet first."
        case .noUserProfile:
            return "No user profile found for this wallet."
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        }
    }
}

protocol UserRepositoryProtocol: AnyObject {
    var userState: UserState { get }
    var userStatePublisher: AnyPublisher<UserState, Never> { get }

    func initialize()
    func fetchAndStoreUser(walletType: WalletType) async throws -> Void
    func getStoredUser() -> User?
    func clearUserProfile() async
    func hasStoredProfile() -> Bool
}

class UserRepository: UserRepositoryProtocol {
    // MARK: - Properties

    private let dataProvider: DataProvider
    private let storageManager: StorageManager
    private let keyManager: KeyManager
    private let enclaveOrchestrator: EnclaveOrchestrator
    private let unifiedSigner: UnifiedSigner

    // MARK: - UserRepositoryProtocol

    var userState: UserState {
        storageManager.userState
    }

    var userStatePublisher: AnyPublisher<UserState, Never> {
        storageManager.$userState.eraseToAnyPublisher()
    }

    // MARK: - Initialization

    init(
        dataProvider: DataProvider,
        storageManager: StorageManager,
        keyManager: KeyManager,
        enclaveOrchestrator: EnclaveOrchestrator,
        unifiedSigner: UnifiedSigner
    ) {
        self.dataProvider = dataProvider
        self.storageManager = storageManager
        self.keyManager = keyManager
        self.enclaveOrchestrator = enclaveOrchestrator
        self.unifiedSigner = unifiedSigner
    }

    // MARK: - Public Methods

    /// Initialize the user repository by loading stored user data and initializing enclave
    func initialize() {
        Logger.repository.debug("UserRepository: Initializing")
        if let user = getStoredUser() {
            Logger.repository.info("UserRepository: Found stored user with enclave type: \(user.enclaveKeyType), wallet type: \(user.walletType.rawValue)")

            // Restore appropriate signer based on wallet type
            do {
                switch user.walletType {
                case .local:
                    unifiedSigner.activateLocalSigner()
                    Logger.repository.info("UserRepository: Activated local signer")
                case .remote:
                    unifiedSigner.activateRemoteSigner()
                    Logger.repository.info("UserRepository: Activated remote signer")
                }

                // Validate that the actual address matches stored address
                let actualAddress = try unifiedSigner.getActiveAddress()
                if actualAddress.lowercased() != user.walletAddress.lowercased() {
                    Logger.repository.warning("UserRepository: Address mismatch, clearing profile")
                    storageManager.clearUserProfile()
                    return
                }

                Logger.repository.info("UserRepository: Signer restored successfully")
            } catch {
                Logger.repository.error("UserRepository: Failed to restore signer: \(error.localizedDescription), clearing profile")
                storageManager.clearUserProfile()
                return
            }

            // Initialize enclave
            if let keyType = EnclaveKeyType.companion.getByValue(type: user.enclaveKeyType) {
                enclaveOrchestrator.initializeType(type: keyType)
                Logger.repository.info("UserRepository: Enclave initialized with type: \(user.enclaveKeyType)")
            } else {
                Logger.repository.error("UserRepository: Invalid enclave type: \(user.enclaveKeyType)")
            }
        } else {
            Logger.repository.debug("UserRepository: No stored user or enclave type found")
        }
    }

    /// Fetches user data from the network and stores it
    func fetchAndStoreUser(walletType: WalletType) async throws -> Void {
        Logger.repository.debug("UserRepository: Starting fetchAndStoreUser with wallet type: \(walletType.rawValue)")

        // 1. Get wallet address from unified signer (not storage!)
        let address: String
        do {
            address = try unifiedSigner.getActiveAddress()
            storageManager.saveWalletAddress(address)
            Logger.repository.debug("UserRepository: Got address from unified signer: \(address, privacy: .private)")
        } catch {
            Logger.repository.error("UserRepository: No active signer found")
            throw UserError.noKeyFound
        }

        // 3. Check if user has profile
        Logger.repository.debug("UserRepository: Checking if user has profile for address: \(address, privacy: .private)")
        do {
            let hasProfile = try await dataProvider.hasUserProfile(address: address)
            guard hasProfile else { throw UserError.noUserProfile }

            let user = try await dataProvider.getUser()

            let userModel = User(
                id: user.id,
                walletAddress: address,
                enclaveKeyType: user.encryptionPasswordStore,
                walletType: walletType
            )

            await MainActor.run {
                Logger.repository.debug("UserRepository: Saving user profile with enclave type: \(user.encryptionPasswordStore), wallet type: \(walletType.rawValue)")
                storageManager.saveUserProfile(userModel)

                // Initialize enclave with user's chosen type
                if let keyType = EnclaveKeyType.companion.getByValue(type: user.encryptionPasswordStore) {
                    enclaveOrchestrator.initializeType(type: keyType)
                    Logger.repository.info("UserRepository: Enclave initialized with type: \(keyType.name)")
                } else {
                    Logger.repository.notice("UserRepository: No enclave type found in user profile, skipping enclave initialization")
                }

                Logger.repository.info("UserRepository: User profile saved")
            }
        } catch {
            Logger.repository.notice("UserRepository: No user profile found")
            throw UserError.noUserProfile
        }
    }
    
    /// Returns the stored user if available
    func getStoredUser() -> User? {
        storageManager.getStoredUser()
    }

    /// Clears the user profile from storage
    func clearUserProfile() async {
        storageManager.clearUserProfile()
        try? keyManager.deleteKey()
        try? await enclaveOrchestrator.lock()
        Logger.repository.info("UserRepository: Cleared user profile and keys")
    }

    /// Checks if a user profile exists in storage
    func hasStoredProfile() -> Bool {
        storageManager.hasUserProfile()
    }
}

// MARK: - Mock Implementation

#if DEBUG
class MockUserRepository: UserRepositoryProtocol {
    private let stateSubject: CurrentValueSubject<UserState, Never>

    var userState: UserState {
        stateSubject.value
    }

    var userStatePublisher: AnyPublisher<UserState, Never> {
        stateSubject.eraseToAnyPublisher()
    }

    init(initialState: UserState = .loadingUser) {
        self.stateSubject = CurrentValueSubject<UserState, Never>(initialState)
    }

    func initialize() {
        // Mock implementation - no-op
    }

    func fetchAndStoreUser(walletType: WalletType) async throws -> Void {
        stateSubject.send(.loadingUser)
        try? await Task.sleep(nanoseconds: 1_000_000_000)  // Simulate network delay

        let mockUser = User(
            id: "user123",
            walletAddress: "0x123...",
            enclaveKeyType: "user",
            walletType: walletType
        )

        stateSubject.send(.connectedUser(user: mockUser))
    }

    func getStoredUser() -> User? {
        if case .connectedUser(let user) = userState {
            return user
        }
        return nil
    }

    func clearUserProfile() {
        stateSubject.send(.noUser)
    }

    func hasStoredProfile() -> Bool {
        if case .connectedUser = userState {
            return true
        }
        return false
    }

}
#endif
