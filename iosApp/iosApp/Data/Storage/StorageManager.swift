import Foundation
import Combine

/// User model matching Android's UserModel
struct UserModel: Codable {
    let id: String
    let address: String
    let createdAt: String?

    enum CodingKeys: String, CodingKey {
        case id
        case address
        case createdAt = "created_at"
    }
}

/// User state matching Android's UserState sealed class
enum UserState: Equatable {
    case loadingUser
    case noUser
    case connectedWallet(address: String)
    case connectedUser(user: UserModel)
    case userError(message: String)

    static func == (lhs: UserState, rhs: UserState) -> Bool {
        switch (lhs, rhs) {
        case (.loadingUser, .loadingUser):
            return true
        case (.noUser, .noUser):
            return true
        case let (.connectedWallet(lAddr), .connectedWallet(rAddr)):
            return lAddr == rAddr
        case let (.connectedUser(lUser), .connectedUser(rUser)):
            return lUser.id == rUser.id
        case let (.userError(lMsg), .userError(rMsg)):
            return lMsg == rMsg
        default:
            return false
        }
    }
}

/// Storage Manager matching Android's StorageManager.kt
/// Uses UserDefaults with Combine for reactive state management
class StorageManager: ObservableObject {
    @Published var userState: UserState = .loadingUser

    private let userDefaults = UserDefaults.standard
    private let userProfileKey = "user_profile"
    private let walletAddressKey = "wallet_address"

    init() {
        loadUserState()
    }

    /// Load user state from storage
    private func loadUserState() {
        if let userData = userDefaults.data(forKey: userProfileKey),
           let user = try? JSONDecoder().decode(UserModel.self, from: userData) {
            userState = .connectedUser(user: user)
        } else if let address = userDefaults.string(forKey: walletAddressKey) {
            userState = .connectedWallet(address: address)
        } else {
            userState = .noUser
        }
    }

    /// Save user profile
    func saveUserProfile(_ user: UserModel) {
        if let encoded = try? JSONEncoder().encode(user) {
            userDefaults.set(encoded, forKey: userProfileKey)
            userDefaults.synchronize()
            userState = .connectedUser(user: user)
        }
    }

    /// Save wallet address
    func saveWalletAddress(_ address: String) {
        userDefaults.set(address, forKey: walletAddressKey)
        userDefaults.synchronize()
        userState = .connectedWallet(address: address)
    }

    /// Get current user if available
    func getCurrentUser() -> UserModel? {
        if case .connectedUser(let user) = userState {
            return user
        }
        return nil
    }

    /// Clear user data
    func clearUserData() {
        userDefaults.removeObject(forKey: userProfileKey)
        userDefaults.removeObject(forKey: walletAddressKey)
        userDefaults.synchronize()
        userState = .noUser
    }
}