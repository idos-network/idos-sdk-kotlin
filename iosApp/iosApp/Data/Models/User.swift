import Foundation

/// Represents a user in the system
struct User: Codable, Equatable {
    let id: String
    let walletAddress: String
    let lastUpdated: TimeInterval
    
    enum CodingKeys: String, CodingKey {
        case id
        case walletAddress = "wallet_address"
        case lastUpdated = "last_updated"
    }
}
