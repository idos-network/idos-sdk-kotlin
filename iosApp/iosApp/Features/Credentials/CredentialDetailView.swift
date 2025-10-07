import SwiftUI

/// CredentialDetailView matching Android's CredentialDetailScreen
struct CredentialDetailView: View {
    @StateObject var viewModel: CredentialDetailViewModel
    @State private var showKeyGenDialog = false
    @State private var showKeyGenError = false

    var body: some View {
        ZStack {
            // Show credential loading/error/content
            if viewModel.state.isLoading {
                LoadingStateView(message: "Loading credential...")
            } else if let error = viewModel.state.error {
                ErrorStateView(
                    message: error,
                    canRetry: true,
                    onRetry: {
                        viewModel.onEvent(.loadCredential)
                    },
                    onReset: {
                        viewModel.onEvent(.lockEnclave)
                    }
                )
            } else if let decrypted = viewModel.state.decryptedContent {
                credentialContent(credential: viewModel.state.credential!, content: decrypted)
            } else if let credential = viewModel.state.credential {
                credentialContent(credential: credential, content: "")
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
        .sheet(isPresented: $showKeyGenDialog) {
            if .hidden != viewModel.enclaveUiState {
                KeyGenerationDialog(
                    isGenerating: viewModel.enclaveUiState == .unlocking,
                    onGenerate: { password, expiration in
                        viewModel.onEvent(.unlockEnclave(password: password, expiration: expiration))
                    },
                    onDismiss: {
                        showKeyGenDialog = false
                        viewModel.onEvent(.dismissEnclave)
                    }
                )
            }
        }
        .onChange(of: viewModel.enclaveUiState) { newState in
            switch newState {
            case .requiresUnlock:
                showKeyGenDialog = true
            case .hidden:
                showKeyGenDialog = false
                // Retry loading credential after key is available
//                if viewModel.state.credential == nil && !viewModel.state.isLoading {
//                    viewModel.onEvent(.loadCredential)
//                }
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

                    // JSON Content or Decrypt Button
                VStack(alignment: .leading, spacing: 8) {
                    Text("Content")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    if !content.isEmpty {
                        // Show decrypted content
                        Text(content.prettyPrintedJSON())
                            .font(.system(.footnote, design: .monospaced))
                            .padding()
                            .background(Color(.systemGray6))
                            .cornerRadius(8)
                    } else {
                        // Show decrypt button
                        VStack(spacing: 12) {
                            Image(systemName: "lock.shield")
                                .font(.system(size: 40))
                                .foregroundColor(.blue)
                            
                            Text("This credential is encrypted")
                                .font(.headline)
                            
                            Button(action: {
                                viewModel.onEvent(.decryptCredential)
                            }) {
                                HStack {
                                    Image(systemName: "lock.open.fill")
                                    Text("Decrypt Content")
                                }
                                .padding(.horizontal, 20)
                                .padding(.vertical, 8)
                            }
                            .buttonStyle(.borderedProminent)
                            .disabled(viewModel.state.isDecrypting)
                            
                            if viewModel.state.isDecrypting {
                                ProgressView()
                                    .padding(.top, 8)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(.systemGray6))
                        .cornerRadius(12)
                    }
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
