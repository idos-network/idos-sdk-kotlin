import SwiftUI

/// CredentialDetailView matching Android's CredentialDetailScreen
struct CredentialDetailView: View {
    @StateObject var viewModel: CredentialDetailViewModel
    @State private var showKeyGenDialog = false

    var body: some View {
        ZStack {
            if viewModel.state.isLoading {
                LoadingStateView(message: "Loading credential...")
            } else if let error = viewModel.state.error {
                ErrorStateView(
                    message: error,
                    canRetry: true,
                    onRetry: {
                        viewModel.onEvent(.loadCredential)
                    }
                )
            } else if let credential = viewModel.state.credential {
                credentialContent(credential)
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
            if case .requiresKey = viewModel.enclaveState {
                KeyGenerationDialog(
                    userId: "mock-user-id",
                    isGenerating: viewModel.enclaveState == .generating,
                    onGenerate: { password, expiration in
                        viewModel.generateKey(
                            userId: "mock-user-id",
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
        .onChange(of: viewModel.enclaveState) { newState in
            switch newState {
            case .requiresKey:
                showKeyGenDialog = true
            case .available:
                showKeyGenDialog = false
            default:
                break
            }
        }
    }

    private func credentialContent(_ credential: Credential) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Credential Type
                VStack(alignment: .leading, spacing: 8) {
                    Text("Type")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(credential.type)
                        .font(.headline)
                }

                Divider()

                // Issuer
                VStack(alignment: .leading, spacing: 8) {
                    Text("Issuer")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(credential.issuer)
                        .font(.body)
                }

                Divider()

                // JSON Content
                VStack(alignment: .leading, spacing: 8) {
                    Text("Content")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(credential.content)
                        .font(.system(.body, design: .monospaced))
                        .padding()
                        .background(Color(.systemGray6))
                        .cornerRadius(8)
                }
            }
            .padding()
        }
    }
}