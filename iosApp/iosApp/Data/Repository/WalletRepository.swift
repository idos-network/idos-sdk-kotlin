import Foundation
import Combine
import idos_sdk

protocol WalletRepositoryProtocol {
    func getWallets() async throws -> [Wallet]
}

class WalletRepository: WalletRepositoryProtocol {
    private let dataProvider: DataProvider
    
    init(dataProvider: DataProvider) {
        self.dataProvider = dataProvider
    }
    
    func getWallets() async throws -> [Wallet] {
        return try await dataProvider.getWallets().map(Wallet.from)
    }
}

#if DEBUG
class MockWalletRepository: WalletRepositoryProtocol {
    func getWallets() async throws -> [Wallet] {
        return [
            Wallet(
                id: "123",
                address: "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb",
                network: "Ethereum"
            )
        ]
    }
}
#endif
