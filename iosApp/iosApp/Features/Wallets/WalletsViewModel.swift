import Foundation

/// Wallets state
struct WalletsState {
    var wallets: [Wallet] = []
    var isLoading: Bool = false
    var error: String? = nil
}

/// Wallets events
enum WalletsEvent {
    case loadWallets
    case refresh
    case clearError
}

/// WalletsViewModel matching Android's WalletsViewModel
class WalletsViewModel: BaseViewModel<WalletsState, WalletsEvent> {
    private let navigationCoordinator: NavigationCoordinator

    init(navigationCoordinator: NavigationCoordinator) {
        self.navigationCoordinator = navigationCoordinator
        super.init(initialState: WalletsState())
        loadWallets()
    }

    override func onEvent(_ event: WalletsEvent) {
        switch event {
        case .loadWallets, .refresh:
            loadWallets()
        case .clearError:
            state.error = nil
        }
    }

    private func loadWallets() {
        state.isLoading = true
        state.error = nil

        // TODO: Fetch wallets from API
        // For now, use mock data
        let mockWallets = [
            Wallet(
                id: "123",
                address: "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb",
                network: "Ethereum"
            )
        ]

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            self?.state.wallets = mockWallets
            self?.state.isLoading = false
        }
    }
}
