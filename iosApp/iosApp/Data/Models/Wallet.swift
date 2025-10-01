import Foundation
import idos_sdk

struct Wallet: Codable, Identifiable {
    let id: String
    let address: String
    let network: String
    
    // Static factory method for creating from response
    static func from(response: GetWalletsResponse) -> Wallet {
        return Wallet(
            id: response.id as! String,
            address: response.address,
            network: response.walletType
        )
    }
}
