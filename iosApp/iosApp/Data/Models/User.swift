import Foundation

/// Wallet type enumeration matching Android
enum WalletType: String, Codable {
    case local = "LOCAL"
    case remote = "REMOTE"
}

/// Represents a user in the system
struct User: Codable, Equatable {
    let id: String
    let walletAddress: String
    let enclaveKeyType: String
    let walletType: WalletType

    enum CodingKeys: String, CodingKey {
        case id
        case walletAddress = "wallet_address"
        case enclaveKeyType = "enclave_key_type"
        case walletType = "wallet_type"
    }
}
