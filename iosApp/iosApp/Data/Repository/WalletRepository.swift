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
        try await dataProvider.getWallets().map { Wallet.from(response: $0) }
    }
}

#if DEBUG
class MockWalletRepository: WalletRepositoryProtocol {
    func getWallets() async throws -> [Wallet] {
        [
            Wallet(
                id: "123",
                address: "0x123...",
                network: "Ethereum"
            )
        ]
    }
}
#endif
