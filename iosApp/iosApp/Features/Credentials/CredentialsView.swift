import SwiftUI

/// CredentialsView matching Android's CredentialsScreen
struct CredentialsView: View {
    @StateObject var viewModel: CredentialsViewModel
    @State private var showKeyGenDialog = false

    var body: some View {
        ZStack {
            if viewModel.state.isLoading {
                LoadingStateView(message: "Loading credentials...")
            } else if let error = viewModel.state.error {
                ErrorStateView(
                    message: error,
                    canRetry: true,
                    onRetry: {
                        viewModel.onEvent(.loadCredentials)
                    }
                )
            } else if viewModel.state.credentials.isEmpty {
                EmptyStateView(
                    icon: "doc.text",
                    title: "No credentials found",
                    subtitle: "Add your first credential to get started"
                )
            } else {
                credentialsList
            }
        }
        .navigationTitle("Credentials")
        .refreshable {
            viewModel.onEvent(.refresh)
        }
        .sheet(isPresented: $showKeyGenDialog) {
            if case .requiresKey = viewModel.enclaveState {
                KeyGenerationDialog(
                    userId: "mock-user-id", // TODO: Get from UserRepository
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

    private var credentialsList: some View {
        List(viewModel.state.credentials) { credential in
            Button(action: {
                viewModel.onEvent(.credentialSelected(credential))
            }) {
                CredentialCard(credential: credential)
            }
            .buttonStyle(PlainButtonStyle())
        }
    }
}

/// Credential Card component
struct CredentialCard: View {
    let credential: Credential

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(credential.type)
                .font(.headline)

            Text("Issuer: \(credential.issuer)")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 8)
    }
}