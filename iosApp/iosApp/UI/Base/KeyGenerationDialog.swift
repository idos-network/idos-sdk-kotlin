import SwiftUI

/// Key Generation Dialog matching Android's KeyGenerationDialog.kt
struct KeyGenerationDialog: View {
    @Environment(\.dismiss) var dismiss
    @State private var password: String = ""
    @State private var showPassword: Bool = false
    @State private var selectedExpiration: KeyExpiration = .oneWeek

    let isGenerating: Bool
    let onGenerate: (String, KeyExpiration) -> Void
    let onDismiss: () -> Void

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Generate Encryption Key")) {
                    HStack {
                        if showPassword {
                            TextField("Password", text: $password)
                                .textContentType(.password)
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
                }

                Section(header: Text("Key Expiration")) {
                    Picker("Expiration", selection: $selectedExpiration) {
                        ForEach(KeyExpiration.allCases, id: \.self) { expiration in
                            Text(expiration.displayName).tag(expiration)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                Section {
                    Button(action: {
                        onGenerate(password, selectedExpiration)
                    }) {
                        if isGenerating {
                            HStack {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                                Text("Generating...")
                            }
                        } else {
                            Text("Generate Key")
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .disabled(password.isEmpty || isGenerating)

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
        }
    }
}
