import Foundation

/// Wallets state matching Android's WalletsState
struct WalletsState {
    var wallets: [Wallet] = []
    var isLoading: Bool = false
    var error: String? = nil
}

/// Wallets events matching Android's WalletsEvent
enum WalletsEvent {
    case loadWallets
    case refresh
    case clearError
}

/// WalletsViewModel matching Android's WalletsViewModel
class WalletsViewModel: BaseViewModel<WalletsState, WalletsEvent> {
    private let walletRepository: WalletRepositoryProtocol
    private let navigationCoordinator: NavigationCoordinator

    init(
        walletRepository: WalletRepositoryProtocol,
        navigationCoordinator: NavigationCoordinator
    ) {
        self.walletRepository = walletRepository
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
        Task { [weak self] in
            guard let self = self else { return }

            await MainActor.run {
                self.state.isLoading = true
                self.state.error = nil
            }

            do {
                let wallets = try await walletRepository.getWallets()
                await MainActor.run {
                    self.state.wallets = wallets
                    self.state.isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.state.error = "Failed to load wallets: \(error.localizedDescription)"
                    self.state.isLoading = false
                }
            }
        }
    }
}
