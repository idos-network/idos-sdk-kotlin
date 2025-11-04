import SwiftUI
import OSLog

/// MnemonicView for wallet import matching Android's MnemonicScreen
struct MnemonicView: View {
    @StateObject var viewModel: MnemonicViewModel
    @EnvironmentObject var diContainer: DIContainer
    @State private var scrollProxy: ScrollViewProxy?

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Text("Import Wallet")
                    .font(.title2)
                    .fontWeight(.bold)
                    .padding(.top, 8)

                Text("Enter your 12 or 24 word recovery phrase")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                // Mnemonic Input
                ZStack(alignment: .topLeading) {
                    if viewModel.state.mnemonic.isEmpty {
                        Text("word1 word2 word3 ...")
                            .foregroundColor(Color(.placeholderText))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 16)
                    }

                    TextEditor(
                        text: Binding(
                            get: { viewModel.state.mnemonic },
                            set: { viewModel.onEvent(.updateMnemonic($0)) }
                        )
                    )
                    .frame(minHeight: 100)
                    .padding(4)
                    .introspectTextView { textView in
                        textView.backgroundColor = .clear
                    }
                }
                .background(Color(.systemGray6))
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color(.systemGray4), lineWidth: 1)
                )
                .autocapitalization(.none)
                .disableAutocorrection(true)

                // Derivation Path Input
                VStack(alignment: .leading, spacing: 8) {
                    Text("Derivation Path")
                        .font(.subheadline)
                        .fontWeight(.medium)

                    TextField(
                        EthSigner.defaultDerivationPath,
                        text: Binding(
                            get: { viewModel.state.derivationPath },
                            set: { viewModel.onEvent(.updateDerivationPath($0)) }
                        )
                    )
                    .padding(8)
                    .background(Color(.systemGray6))
                    .cornerRadius(8)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color(.systemGray4), lineWidth: 1)
                    )
                    .autocapitalization(.none)
                    .disableAutocorrection(true)
                    .accessibilityIdentifier("derivationPath")
                }

                // Import Button
                Button(action: { viewModel.onEvent(.importWallet) }) {
                    Text("Import Wallet")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(viewModel.state.mnemonic.isEmpty ? Color.gray : Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
                .disabled(viewModel.state.mnemonic.isEmpty || viewModel.state.isLoading)
                .id("importButton")

                Spacer(minLength: 20)
            }
            .padding(.horizontal, 32)
            .padding(.top, 32)
            .padding(.bottom, 20)
            }
            .onAppear {
                scrollProxy = proxy
            }
        }
        .navigationTitle("Recovery Phrase")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Done") {
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                }
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)) { _ in
            withAnimation {
                scrollProxy?.scrollTo("importButton", anchor: .bottom)
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)) { _ in
            // Optionally scroll back to top when keyboard hides
        }
        .overlay(
            Group {
                if viewModel.state.isLoading || viewModel.state.isSuccess || viewModel.state.error != nil {
                    ZStack {
                        // Dimmed background
                        Color.black.opacity(0.4)
                            .ignoresSafeArea()

                        // Dialog card
                        VStack(spacing: 16) {
                            if viewModel.state.isLoading && !viewModel.state.isSuccess {
                                // Loading state during initial import
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                                    .scaleEffect(1.5)
                                    .padding()
                                Text("Processing Wallet")
                                    .font(.headline)
                                Text("Please wait while we import your wallet...")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                                    .multilineTextAlignment(.center)
                            } else if viewModel.state.isSuccess {
                                // Success state - clicking OK will trigger fetch
                                Text("Wallet Imported")
                                    .font(.headline)
                                Text("Your wallet has been successfully imported.")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                                    .multilineTextAlignment(.center)

                                if viewModel.state.isLoading {
                                    // Loading state during user fetch
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle())
                                        .scaleEffect(1.2)
                                        .padding(.top, 8)
                                    Text("Loading profile...")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                } else {
                                    Button("OK") {
                                        Logger.viewModel.debug("MnemonicView: User clicked OK, triggering user fetch")
                                        viewModel.onEvent(.fetchUser)
                                    }
                                    .buttonStyle(.borderedProminent)
                                    .padding(.top, 8)
                                }
                            } else if let error = viewModel.state.error {
                                // Error state
                                Text("Import Failed")
                                    .font(.headline)
                                Text(error)
                                    .font(.subheadline)
                                    .foregroundColor(.red)
                                    .multilineTextAlignment(.center)

                                Button("Close") {
                                    viewModel.onEvent(.clearError)
                                }
                                .buttonStyle(.bordered)
                                .padding(.top, 8)
                            }
                        }
                        .padding(24)
                        .background(
                            RoundedRectangle(cornerRadius: 16)
                                .fill(Color(.systemBackground))
                                .shadow(radius: 20)
                        )
                        .padding(32)
                    }
                    .transition(.opacity)
                }
            }
        )
    }
}
