import Foundation

/// User state matching Android's UserState sealed class
enum UserState: Equatable {
    case loadingUser
    case noUser
    case connectedWallet(address: String)
    case connectedUser(user: User)
    case userError(message: String)
}

/// Storage Manager matching Android's StorageManager.kt
/// Uses UserDefaults with Combine for reactive state management
class StorageManager: ObservableObject {
    // MARK: - Properties
    
    @Published private(set) var userState: UserState = .loadingUser
    
    private let userDefaults: UserDefaults
    private let jsonEncoder = JSONEncoder()
    private let jsonDecoder = JSONDecoder()
    
    private enum Keys {
        static let userProfile = "user_profile"
    }
    
    // MARK: - Initialization
    
    init(userDefaults: UserDefaults = .standard) {
        self.userDefaults = userDefaults
        loadUserState()
    }
    
    // MARK: - Public Methods
    
    /// Saves the user profile and updates the state
    func saveUserProfile(_ user: User) {
        print("💾 StorageManager: Saving user profile (id: \(user.id), address: \(user.walletAddress))")
        do {
            let data = try jsonEncoder.encode(user)
            userDefaults.set(data, forKey: Keys.userProfile)
            userState = .connectedUser(user: user)
            print("✅ StorageManager: User profile saved, state updated to .connectedUser")
        } catch {
            print("❌ StorageManager: Failed to save user profile: \(error)")
            userState = .userError(message: "Failed to save user profile: \(error.localizedDescription)")
        }
    }
    
    /// Saves the wallet address and updates the state
    func saveWalletAddress(_ address: String) {
        print("💾 StorageManager: Saving wallet address: \(address)")
        userState = .connectedWallet(address: address)
        print("✅ StorageManager: State updated to .connectedWallet")
    }
    
    /// Clears the user profile and updates the state
    func clearUserProfile() {
        userDefaults.removeObject(forKey: Keys.userProfile)
        userState = .noUser
        print("✅ Cleared user profile")
    }
    
    /// Returns the stored user if available
    func getStoredUser() -> User? {
        if case let .connectedUser(user) = userState {
            return user
        }
        return nil
    }
    
    /// Returns the stored wallet address if available
    func getStoredWallet() -> String? {
        switch userState {
        case .connectedUser(let user):
            return user.walletAddress
        case .connectedWallet(let address):
            return address
        case .noUser, .loadingUser, .userError:
            return nil
        }
    }
    
    /// Checks if a user profile exists
    func hasUserProfile() -> Bool {
        if case .connectedUser = userState {
            return true
        }
        return false
    }
    
    // MARK: - Private Methods
    
    private func loadUserState() {
        if let userData = userDefaults.data(forKey: Keys.userProfile),
           let user = try? jsonDecoder.decode(User.self, from: userData) {
            userState = .connectedUser(user: user)
            print("✅ Loaded user profile: \(user.id)")
        } else {
            userState = .noUser
            print("ℹ️ No user or wallet found in storage")
        }
    }
}
