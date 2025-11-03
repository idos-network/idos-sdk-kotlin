import SwiftUI
import idos_sdk

/// CredentialDetailView matching Android's CredentialDetailScreen
struct CredentialDetailView: View {
    @StateObject var viewModel: CredentialDetailViewModel
    @State private var showKeyGenDialog = false

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
            enclaveDialogContent()
        }
        .onChange(of: viewModel.enclaveUiState) { newState in
            switch newState {
            case .requiresUnlock, .unlocking, .unlockError:
                showKeyGenDialog = true
            case .hidden:
                showKeyGenDialog = false
            }
        }
    }

    @ViewBuilder
    private func enclaveDialogContent() -> some View {
        switch viewModel.enclaveUiState {
        case .hidden:
            KeyGenerationDialog(
                enclaveType: .user,
                isGenerating: false,
                error: nil,
                canRetry: true,
                onGenerate: { password, expiration in
                    let config = EnclaveSessionConfig(
                        expirationType: .timed,
                        expirationMillis: KotlinLong(value: expiration.rawValue)
                    )
                    viewModel.onEvent(.unlockEnclave(password: password, config: config))
                },
                onDismiss: {
                    showKeyGenDialog = false
                    viewModel.onEvent(.dismissEnclave)
                }
            )
        case .requiresUnlock(let type):
            KeyGenerationDialog(
                enclaveType: type,
                isGenerating: false,
                error: nil,
                canRetry: true,
                onGenerate: { password, expiration in
                    let config = EnclaveSessionConfig(
                        expirationType: .timed,
                        expirationMillis: KotlinLong(value: expiration.rawValue)
                    )
                    viewModel.onEvent(.unlockEnclave(password: password, config: config))
                },
                onDismiss: {
                    showKeyGenDialog = false
                    viewModel.onEvent(.dismissEnclave)
                }
            )
        case .unlocking(let type):
            KeyGenerationDialog(
                enclaveType: type,
                isGenerating: true,
                error: nil,
                canRetry: true,
                onGenerate: { password, expiration in
                    let config = EnclaveSessionConfig(
                        expirationType: .timed,
                        expirationMillis: KotlinLong(value: expiration.rawValue)
                    )
                    viewModel.onEvent(.unlockEnclave(password: password, config: config))
                },
                onDismiss: {
                    showKeyGenDialog = false
                    viewModel.onEvent(.dismissEnclave)
                }
            )
        case .unlockError(let type, let message, let retry):
            KeyGenerationDialog(
                enclaveType: type,
                isGenerating: false,
                error: message,
                canRetry: retry,
                onGenerate: { password, expiration in
                    let config = EnclaveSessionConfig(
                        expirationType: .timed,
                        expirationMillis: KotlinLong(value: expiration.rawValue)
                    )
                    viewModel.onEvent(.unlockEnclave(password: password, config: config))
                },
                onDismiss: {
                    showKeyGenDialog = false
                    viewModel.onEvent(.dismissEnclave)
                }
            )
        }
    }

    private func credentialContent(credential: CredentialDetail, content: String) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // ID Section
                VStack(alignment: .leading, spacing: 4) {
                    Text("ID")
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.secondary)
                    Text(credential.id)
                        .font(.body)
                }

                // JSON Content or Decrypt Button
                VStack(alignment: .leading, spacing: 8) {
                    Text("Content")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(.blue)

                    if !content.isEmpty {
                        // Show decrypted content with dynamic JSON rendering
                        if let jsonValue = content.parseJSON() {
                            JsonElementDisplay(jsonValue: jsonValue)
                        } else {
                            // Fallback to plain text if JSON parsing fails
                            Text(content)
                                .font(.system(.footnote, design: .monospaced))
                                .padding()
                                .background(Color(.systemGray6))
                                .cornerRadius(8)
                        }
                    } else {
                        // Show decrypt button
                        VStack(spacing: 12) {
                            Image(systemName: "lock")
                                .font(.system(size: 48))
                                .foregroundColor(.blue)

                            Text("This credential is encrypted")
                                .font(.headline)

                            Button(action: {
                                viewModel.onEvent(.decryptCredential)
                            }) {
                                Text("Decrypt Content")
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
                        .padding(32)
                        .background(Color(.systemGray6))
                        .cornerRadius(8)
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
