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

    func fetchAndStoreUser() async throws
    func getStoredUser() -> User?
    func clearUserProfile()
    func hasStoredProfile() -> Bool
    func saveWalletAddress(_ address: String)
}

class UserRepository: UserRepositoryProtocol {
    // MARK: - Properties

    private let dataProvider: DataProvider
    private let storageManager: StorageManager
    private let keyManager: KeyManager

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
        keyManager: KeyManager
    ) {
        self.dataProvider = dataProvider
        self.storageManager = storageManager
        self.keyManager = keyManager
    }

    // MARK: - Public Methods

    /// Fetches user data from the network and stores it
    func fetchAndStoreUser() async throws {
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

        do {
            // 3. Check if user has profile
            print("🔍 UserRepository: Checking if user has profile for address: \(address)")
            let hasProfile = try await dataProvider.hasUserProfile(address: address.addressWithPrefix())
            guard hasProfile else {
                print("⚠️ UserRepository: No user profile found")
                throw UserError.noUserProfile
            }
            print("✅ UserRepository: User profile exists")

            // 4. Fetch user data
            print("📥 UserRepository: Fetching user data")
            let user = try await dataProvider.getUser()

            // 5. Convert to User and save
            let userModel = User(
                id: user.id as! String,
                walletAddress: address,
                lastUpdated: 0
            )

            await MainActor.run {
                print("💾 UserRepository: Saving user profile to storage")
                storageManager.saveUserProfile(userModel)
                print("✅ UserRepository: User profile saved successfully")
            }

        } catch let error as UserError {
            throw error
        } catch {
            print("❌ UserRepository: Network error - \(error.localizedDescription)")
            throw UserError.networkError(error)
        }
    }

    /// Returns the stored user if available
    func getStoredUser() -> User? {
        storageManager.getStoredUser()
    }

    /// Clears the user profile from storage
    func clearUserProfile() {
        storageManager.clearUserProfile()
    }

    /// Checks if a user profile exists in storage
    func hasStoredProfile() -> Bool {
        storageManager.hasUserProfile()
    }

    /// Saves the wallet address to storage
    func saveWalletAddress(_ address: String) {
        storageManager.saveWalletAddress(address)
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

        func fetchAndStoreUser() async throws {
            stateSubject.send(.loadingUser)
            try? await Task.sleep(nanoseconds: 1_000_000_000)  // Simulate network delay

            let mockUser = User(
                id: "user123",
                walletAddress: "0x123...",
                lastUpdated: 0
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

        func saveWalletAddress(_ address: String) {
            stateSubject.send(.connectedWallet(address: address))
        }
    }
#endif
