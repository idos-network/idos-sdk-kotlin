import SwiftUI

/// SettingsView matching Android's SettingsScreen
struct SettingsView: View {
    @StateObject var viewModel: SettingsViewModel
    @EnvironmentObject var diContainer: DIContainer

    var body: some View {
        List {
            Section(header: Text("Security")) {
                HStack {
                    Label("Encryption Key", systemImage: "key")
                    Spacer()
                    if viewModel.state.hasEncryptionKey {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(.green)
                    } else {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.red)
                    }
                }

                if viewModel.state.hasEncryptionKey {
                    Button(role: .destructive, action: {
                        viewModel.onEvent(.deleteEncryptionKey)
                    }) {
                        if viewModel.state.isDeleting {
                            HStack {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                                Text("Deleting...")
                            }
                        } else {
                            Label("Delete Encryption Key", systemImage: "trash")
                        }
                    }
                    .disabled(viewModel.state.isDeleting)
                }
            }

            Section(header: Text("Account")) {
                Button(role: .destructive, action: {
                    viewModel.onEvent(.disconnectWallet)
                }) {
                    Label("Disconnect Wallet", systemImage: "arrow.right.square")
                }
            }

            Section(header: Text("About")) {
                HStack {
                    Text("Version")
                    Spacer()
                    Text("1.0.0")
                        .foregroundColor(.secondary)
                }

                HStack {
                    Text("Bundle ID")
                    Spacer()
                    Text("org.idos.app")
                        .foregroundColor(.secondary)
                        .font(.caption)
                }
            }
        }
        .navigationTitle("Settings")
        .onAppear {
            viewModel.onEvent(.checkKeyStatus)
        }
        .alert("Delete Encryption Key", isPresented: $viewModel.state.showDeleteConfirmation) {
            Button("Cancel", role: .cancel) {
                viewModel.onEvent(.cancelDelete)
            }
            Button("Delete", role: .destructive) {
                viewModel.onEvent(.confirmDelete)
            }
        } message: {
            Text("Are you sure you want to delete your encryption key? You will need to generate a new key to decrypt credentials.")
        }
        .alert("Error", isPresented: .constant(viewModel.state.error != nil)) {
            Button("OK") {
                viewModel.onEvent(.clearError)
            }
        } message: {
            if let error = viewModel.state.error {
                Text(error)
            }
        }
    }
}
