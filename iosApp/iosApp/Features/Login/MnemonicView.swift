import SwiftUI

/// MnemonicView for wallet import matching Android's MnemonicScreen
struct MnemonicView: View {
    @EnvironmentObject var diContainer: DIContainer
    @State private var mnemonic: String = ""
    @State private var isImporting: Bool = false
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Text("Import Wallet")
                    .font(.title2)
                    .fontWeight(.bold)

                Text("Enter your 12 or 24 word recovery phrase")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                // Mnemonic Input
                TextEditor(text: $mnemonic)
                    .frame(minHeight: 150)
                    .padding(8)
                    .background(Color(.systemGray6))
                    .cornerRadius(8)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color(.systemGray4), lineWidth: 1)
                    )
                    .autocapitalization(.none)
                    .disableAutocorrection(true)

                if let errorMessage = errorMessage {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundColor(.red)
                }

                // Import Button
                Button(action: importWallet) {
                    if isImporting {
                        HStack {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            Text("Importing...")
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue.opacity(0.6))
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    } else {
                        Text("Import Wallet")
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(mnemonic.isEmpty ? Color.gray : Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }
                }
                .disabled(mnemonic.isEmpty || isImporting)

                Spacer()
            }
            .padding(32)
        }
        .navigationTitle("Recovery Phrase")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func importWallet() {
        isImporting = true
        errorMessage = nil

        // TODO: Implement BIP39/BIP44 mnemonic derivation
        // For now, this is a placeholder that stores a mock address
        Task {
            do {
                // Validate mnemonic (12 or 24 words)
                let words = mnemonic.trimmingCharacters(in: .whitespacesAndNewlines)
                    .components(separatedBy: .whitespaces)
                    .filter { !$0.isEmpty }

                guard words.count == 12 || words.count == 24 else {
                    await MainActor.run {
                        errorMessage = "Please enter a valid 12 or 24 word recovery phrase"
                        isImporting = false
                    }
                    return
                }

                // TODO: Derive Ethereum key using BIP39/BIP44
                // This requires a Swift crypto library like web3.swift

                // For now, save a mock wallet address
                let mockAddress = "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb"

                await MainActor.run {
                    diContainer.storageManager.saveWalletAddress(mockAddress)
                    isImporting = false

                    // Navigate to dashboard
                    diContainer.navigationCoordinator.replace(with: .dashboard)
                }
            }
        }
    }
}