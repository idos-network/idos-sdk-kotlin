import SwiftUI
import idos_sdk

/// Key Generation Dialog matching Android's KeyGenerationDialog.kt
/// Supports both USER (local) and MPC enclave types
struct KeyGenerationDialog: View {
    @Environment(\.dismiss) var dismiss
    @State private var password: String = ""
    @State private var showPassword: Bool = false
    @State private var selectedExpiration: KeyExpiration = .oneWeek

    let enclaveType: EnclaveKeyType
    let isGenerating: Bool
    let error: String?
    let canRetry: Bool
    let onGenerate: (String?, KeyExpiration) -> Void
    let onDismiss: () -> Void

    // Password is required only for USER enclave type
    private var requiresPassword: Bool {
        enclaveType == .user
    }

    // Skip password validation for now (can add back: password.count >= 8)
    private var isPasswordValid: Bool {
        !requiresPassword || true
    }

    private var canGenerate: Bool {
        isPasswordValid && !isGenerating && canRetry
    }

    var body: some View {
        NavigationView {
            Form {
                // Title and description
                Section {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(requiresPassword ? "Generate Encryption Key" : "Unlock MPC Enclave")
                            .font(.headline)

                        Text(requiresPassword
                            ? "Provide the password to generate your encryption key."
                            : "Select session duration to unlock your MPC enclave.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 4)
                }

                // Password field (only for USER enclave)
                if requiresPassword {
                    Section(header: Text("Password")) {
                        HStack {
                            if showPassword {
                                TextField("Password", text: $password)
                                    .textContentType(.password)
                                    .autocapitalization(.none)
                            } else {
                                SecureField("Password", text: $password)
                                    .textContentType(.password)
                            }

                            Button(action: {
                                showPassword.toggle()
                            }) {
                                Image(systemName: showPassword ? "eye.slash" : "eye")
                                    .foregroundColor(.secondary)
                            }
                        }
                        .disabled(isGenerating)

                        if !password.isEmpty && !isPasswordValid {
                            Text("Password must be at least 8 characters")
                                .font(.caption)
                                .foregroundColor(.red)
                        }
                    }
                }

                // Expiration selection
                Section(header: Text(requiresPassword ? "Key Expiration" : "Session Duration")) {
                    ForEach(KeyExpiration.allCases, id: \.self) { expiration in
                        Button(action: {
                            selectedExpiration = expiration
                        }) {
                            HStack {
                                Text(expiration.displayName)
                                    .foregroundColor(.primary)
                                Spacer()
                                if selectedExpiration == expiration {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(.blue)
                                }
                            }
                        }
                        .disabled(isGenerating)
                    }
                }

                // Error message
                if let errorMessage = error {
                    Section {
                        HStack(alignment: .top, spacing: 8) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.red)
                            Text(errorMessage)
                                .font(.subheadline)
                                .foregroundColor(.red)
                        }
                        .padding(.vertical, 4)
                    }
                }

                // Action buttons
                Section {
                    Button(action: {
                        onGenerate(
                            requiresPassword ? password : nil,
                            selectedExpiration
                        )
                    }) {
                        HStack {
                            if isGenerating {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                                Spacer()
                            }
                            Text(requiresPassword ? "Generate Key" : "Unlock")
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .disabled(!canGenerate)

                    Button("Cancel") {
                        onDismiss()
                        dismiss()
                    }
                    .foregroundColor(.red)
                    .frame(maxWidth: .infinity)
                    .disabled(isGenerating)
                }
            }
            .navigationTitle("Encryption Key")
            .navigationBarTitleDisplayMode(.inline)
            .interactiveDismissDisabled(isGenerating)
        }
    }
}
