import SwiftUI

/// CredentialsView matching Android's CredentialsScreen
struct CredentialsView: View {
    @StateObject var viewModel: CredentialsViewModel

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
    }

    private var credentialsList: some View {
        List(viewModel.state.credentials) { credential in
            Button(action: {
                viewModel.onEvent(.credentialSelected(credential))
            }) {
                CredentialCard(credential: credential)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
    }
}

/// Credential Card component
struct CredentialCard: View {
    let credential: Credential

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 8) {
                Text(credential.type)
                    .font(.headline)

                Text("Issuer: \(credential.issuer)")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 8)
    }
}

#Preview {
    let diContainer = DIContainer.shared
    NavigationView {
        CredentialsView(
            viewModel: diContainer.makeCredentialsViewModel()
        )
    }
    .environmentObject(diContainer)
    .environmentObject(diContainer.navigationCoordinator)
}