import SwiftUI

/// WalletsView matching Android's WalletsScreen
struct WalletsView: View {
    @StateObject var viewModel: WalletsViewModel

    var body: some View {
        ZStack {
            if viewModel.state.isLoading {
                LoadingStateView(message: "Loading wallets...")
            } else if let error = viewModel.state.error {
                ErrorStateView(
                    message: error,
                    canRetry: true,
                    onRetry: {
                        viewModel.onEvent(.loadWallets)
                    }
                )
            } else if viewModel.state.wallets.isEmpty {
                EmptyStateView(
                    icon: "wallet.pass",
                    title: "No wallets found",
                    subtitle: "Connect a wallet to get started"
                )
            } else {
                walletsList
            }
        }
        .navigationTitle("Wallets")
        .refreshable {
            viewModel.onEvent(.refresh)
        }
    }

    private var walletsList: some View {
        List(viewModel.state.wallets) { wallet in
            WalletCard(wallet: wallet)
        }
    }
}

/// Wallet Card component
struct WalletCard: View {
    let wallet: Wallet

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(wallet.type)
                .font(.headline)

            Text(wallet.address)
                .font(.system(.subheadline, design: .monospaced))
                .foregroundColor(.secondary)
                .lineLimit(1)
                .truncationMode(.middle)
        }
        .padding(.vertical, 8)
    }
}