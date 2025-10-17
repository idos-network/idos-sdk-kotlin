import Combine
import Foundation
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
    func fetchAndStoreUser() async throws -> Void
    func getStoredUser() -> User?
    func clearUserProfile()
    func hasStoredProfile() -> Bool
}

class UserRepository: UserRepositoryProtocol {
    // MARK: - Properties

    private let dataProvider: DataProvider
    private let storageManager: StorageManager
    private let keyManager: KeyManager
    private let enclaveOrchestrator: EnclaveOrchestrator

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
        enclaveOrchestrator: EnclaveOrchestrator
    ) {
        self.dataProvider = dataProvider
        self.storageManager = storageManager
        self.keyManager = keyManager
        self.enclaveOrchestrator = enclaveOrchestrator
    }

    // MARK: - Public Methods

    /// Initialize the user repository by loading stored user data and initializing enclave
    func initialize() {
        print("🔄 UserRepository: Initializing")
        if let user = getStoredUser() {
            print("✅ UserRepository: Found stored user with enclave type: \(user.enclaveKeyType)")
            if let keyType = EnclaveKeyType.companion.getByValue(type: user.enclaveKeyType) {
                enclaveOrchestrator.initializeType(type: keyType)
                print("✅ UserRepository: Enclave initialized with type: \(user.enclaveKeyType)")
            } else {
                print("⚠️ UserRepository: Invalid enclave type: \(user.enclaveKeyType)")
            }
        } else {
            print("ℹ️ UserRepository: No stored user or enclave type found")
        }
    }

    /// Fetches user data from the network and stores it
    func fetchAndStoreUser() async throws -> Void {
        print("🔄 UserRepository: Starting fetchAndStoreUser")

        // 1. Get wallet address from storage
        guard let address = storageManager.getStoredWallet() else {
            print("❌ UserRepository: No wallet address found in storage")
            throw UserError.noKeyFound
        }
        print("📍 UserRepository: Using wallet address: \(address)")

        // 2. Get key from key manager to verify it exists
        do {
            _ = try keyManager.getKey()
            print("✅ UserRepository: Private key verified in keychain")
        } catch {
            print("❌ UserRepository: No key found in keychain")
            throw UserError.noKeyFound
        }

        // 3. Check if user has profile
        print("🔍 UserRepository: Checking if user has profile for address: \(address)")
        do {
            let hasProfile = try await dataProvider.hasUserProfile(address: address)
            guard hasProfile else { throw UserError.noUserProfile }

            let user = try await dataProvider.getUser()

            let userModel = User(
                id: user.id,
                walletAddress: address,
                enclaveKeyType: user.encryptionPasswordStore)

            await MainActor.run {
                print("💾 Saving user profile with enclave type: \(user.encryptionPasswordStore)")
                storageManager.saveUserProfile(userModel)
                
                // Initialize enclave with user's chosen type
                if let keyType = EnclaveKeyType.companion.getByValue(type: user.encryptionPasswordStore) {
                    enclaveOrchestrator.initializeType(type: keyType)
                    print("✅ Enclave initialized with type: \(keyType)")
                } else {
                    print("⚠️ No enclave type found in user profile, skipping enclave initialization")
                }
                
                print("✅ User profile saved")
            }
        } catch {
            print("⚠️ No user profile found")
            throw UserError.noUserProfile
        }
    }
    
    /// Returns the stored user if available
    func getStoredUser() -> User? {
        storageManager.getStoredUser()
    }

    /// Clears the user profile from storage
    func clearUserProfile() {
        storageManager.clearUserProfile()
        try? keyManager.deleteKey()
        print("✅ Cleared user profile and keys")
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

    func fetchAndStoreUser() async throws -> Void {
        stateSubject.send(.loadingUser)
        try? await Task.sleep(nanoseconds: 1_000_000_000)  // Simulate network delay

        let mockUser = User(
            id: "user123",
            walletAddress: "0x123...",
            enclaveKeyType: "user",
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
