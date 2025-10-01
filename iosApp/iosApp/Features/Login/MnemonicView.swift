import SwiftUI

/// MnemonicView for wallet import matching Android's MnemonicScreen
struct MnemonicView: View {
    @StateObject var viewModel: MnemonicViewModel
    @EnvironmentObject var diContainer: DIContainer

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
                TextEditor(
                    text: Binding(
                        get: { viewModel.state.mnemonic },
                        set: { viewModel.onEvent(.updateMnemonic($0)) }
                    )
                )
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

                if let errorMessage = viewModel.state.error {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundColor(.red)
                }

                // Import Button
                Button(action: { viewModel.onEvent(.importWallet) }) {
                    if viewModel.state.isLoading {
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
                            .background(viewModel.state.mnemonic.isEmpty ? Color.gray : Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }
                }
                .disabled(viewModel.state.mnemonic.isEmpty || viewModel.state.isLoading)

                Spacer()
            }
            .padding(32)
        }
        .navigationTitle("Recovery Phrase")
        .navigationBarTitleDisplayMode(.inline)
        .alert(
            "Success!",
            isPresented: Binding<Bool>(
                get: { viewModel.state.isSuccess },
                set: { _ in  }
            )
        ) {
            Button("OK") {
                print("💾 MnemonicView: User clicked OK, triggering user fetch")
                viewModel.onEvent(.fetchUser)
            }
        } message: {
            Text("Wallet imported successfully!")
        }
    }
}
