import SwiftUI

/// CredentialDetailView matching Android's CredentialDetailScreen
struct CredentialDetailView: View {
    @StateObject var viewModel: CredentialDetailViewModel
    @State private var showKeyGenDialog = false
    @State private var showKeyGenError = false

    var body: some View {
        ZStack {
            // Show enclave loading state
            if case .loading = viewModel.enclaveState {
                LoadingStateView(message: "Checking encryption key...")
            }
            // Show enclave error state
            else if case .error(let message, let canRetry) = viewModel.enclaveState {
                ErrorStateView(
                    message: message,
                    canRetry: canRetry,
                    onRetry: {
                        viewModel.retryEnclaveCheck()
                    }
                )
            }
            // Show credential loading/error/content
            else if viewModel.state.isLoading {
                LoadingStateView(message: "Loading credential...")
            } else if let error = viewModel.state.error {
                ErrorStateView(
                    message: error,
                    canRetry: true,
                    onRetry: {
                        viewModel.onEvent(.loadCredential)
                    },
                    onReset: {
                        viewModel.onEvent(.resetKey)
                    }
                )
            } else if let decrypted = viewModel.state.decryptedContent {
                credentialContent(credential: viewModel.state.credential!, content: decrypted)
            } else {
                EmptyStateView(
                    icon: "doc.text",
                    title: "Credential not found",
                    subtitle: "This credential could not be loaded"
                )
            }
        }
        .navigationTitle("Credential Detail")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: {
                    viewModel.onEvent(.copyContent)
                }) {
                    Image(systemName: viewModel.state.copySuccess ? "checkmark" : "doc.on.doc")
                }
                .disabled(viewModel.state.credential == nil)
            }
        }
        .sheet(isPresented: $showKeyGenDialog) {
            if case .requiresKey = viewModel.enclaveState, let userId = viewModel.userId {
                KeyGenerationDialog(
                    userId: userId,
                    isGenerating: viewModel.enclaveState == .generating,
                    onGenerate: { password, expiration in
                        viewModel.generateKey(
                            userId: userId,
                            password: password,
                            expiration: expiration
                        )
                    },
                    onDismiss: {
                        showKeyGenDialog = false
                    }
                )
            }
        }
        .alert("Key Generation Error", isPresented: $showKeyGenError) {
            Button("OK") {
                viewModel.clearEnclaveError()
                showKeyGenError = false
            }
        } message: {
            if case .keyGenerationError(let message) = viewModel.enclaveState {
                Text(message)
            }
        }
        .onChange(of: viewModel.enclaveState) { newState in
            switch newState {
            case .requiresKey:
                showKeyGenDialog = true
            case .available:
                showKeyGenDialog = false
                // Retry loading credential after key is available
                if viewModel.state.credential == nil && !viewModel.state.isLoading {
                    viewModel.onEvent(.loadCredential)
                }
            case .keyGenerationError:
                showKeyGenError = true
            default:
                break
            }
        }
    }

    private func credentialContent(credential: CredentialDetail, content: String) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Issuer
                VStack(alignment: .leading, spacing: 8) {
                    Text("Id")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(credential.id)
                        .font(.body)
                }

                Divider()

                // JSON Content
                VStack(alignment: .leading, spacing: 8) {
                    Text("Content")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(content.prettyPrintedJSON())
                        .font(.system(.footnote, design: .monospaced))
                        .padding()
                        .background(Color(.systemGray6))
                        .cornerRadius(8)
                }
            }
            .padding()
        }
    }
}

#Preview {
    let diContainer = DIContainer.shared
    NavigationView {
        CredentialDetailView(
            viewModel: diContainer.makeCredentialDetailViewModel(credentialId: "1")
        )
    }
}
