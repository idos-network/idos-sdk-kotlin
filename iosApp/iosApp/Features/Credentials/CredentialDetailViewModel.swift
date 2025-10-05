import Foundation
import UIKit
import idos_sdk
import Combine

/// Credential Detail state
struct CredentialDetailState {
    var credential: CredentialDetail?
    var decryptedContent: String? = nil
    var isLoading: Bool = false
    var error: String? = nil
    var copySuccess: Bool = false
}

/// Credential Detail events
enum CredentialDetailEvent {
    case loadCredential
    case copyContent
    case clearError
    case resetKey
}

/// CredentialDetailViewModel extending BaseEnclaveViewModel
class CredentialDetailViewModel: BaseEnclaveViewModel<CredentialDetailState, CredentialDetailEvent> {
    private let credentialId: String
    private let navigationCoordinator: NavigationCoordinator
    private let credentialsRepository: CredentialsRepositoryProtocol
    private let userRepository: UserRepositoryProtocol
    private var cancellables = Set<AnyCancellable>()

    init(
        credentialId: String,
        orchestrator: EnclaveOrchestrator,
        credentialsRepository: CredentialsRepositoryProtocol,
        userRepository: UserRepositoryProtocol,
        navigationCoordinator: NavigationCoordinator
    ) {
        print("📄 CredentialDetailViewModel: Initializing for credentialId: \(credentialId)")
        self.credentialId = credentialId
        self.navigationCoordinator = navigationCoordinator
        self.credentialsRepository = credentialsRepository
        self.userRepository = userRepository
        super.init(
            initialState: CredentialDetailState(),
            orchestrator: orchestrator
        )

        if let userId = userId {
            print("📄 CredentialDetailViewModel: Found stored userId: \(userId)")
        } else {
            print("⚠️ CredentialDetailViewModel: No stored userId found")
        }

        loadCredential()
    }

    var userId: String? {
        userRepository.getStoredUser()?.id
    }

    override func onEvent(_ event: CredentialDetailEvent) {
        switch event {
        case .loadCredential:
            loadCredential()
        case .copyContent:
            copyToClipboard()
        case .clearError:
            state.error = nil
        case .resetKey:
            resetKey()
            loadCredential()
        }
    }

    private func loadCredential() {
        print("📄 CredentialDetailViewModel: Starting loadCredential for id: \(credentialId)")
        Task { [weak self] in
            guard let self = self else { return }

            await MainActor.run {
                self.state.isLoading = true
                self.state.error = nil
                print("📄 CredentialDetailViewModel: State set to loading")
            }

            do {
                // Fetch credential from repository
                print("📄 CredentialDetailViewModel: Fetching credential from repository")
                let credentialDetail = try await credentialsRepository.getCredential(id: credentialId)
                print("✅ CredentialDetailViewModel: Credential fetched successfully")
                print("📄 CredentialDetailViewModel: Credential id: \(credentialDetail.id), pubkey: \(credentialDetail.encryptorPublicKey)")

                // Decrypt the content using enclave
                print("🔐 CredentialDetailViewModel: Starting decryption with enclave")
                try await requireEnclave { [weak self] enclave in
                    guard let self = self else { return }
                    print("🔐 CredentialDetailViewModel: Converting encrypted data")
                    let encryptedData = Data(base64Encoded: credentialDetail.content) ?? Data()
                    let senderPublicKey = Data(base64Encoded: credentialDetail.encryptorPublicKey) ?? Data()

                    print("🔐 CredentialDetailViewModel: Calling enclave.decrypt()")
                    print("🔐 CredentialDetailViewModel: Encrypted data size: \(encryptedData.count) bytes")
                    print("🔐 CredentialDetailViewModel: Sender public key size: \(senderPublicKey.count) bytes")

                    // Set loading state when action actually starts
                    await MainActor.run {
                        self.state.isLoading = true
                    }

                    let decrypted = try await enclave.decrypt(
                        message: encryptedData.toKotlinByteArray(),
                        senderPublicKey: senderPublicKey.toKotlinByteArray()
                    )

                    print("✅ CredentialDetailViewModel: Decryption successful")
                    let decryptedContent = String(data: decrypted.toData(), encoding: .utf8)
                    print("📄 CredentialDetailViewModel: Decrypted content length: \(decryptedContent?.count ?? 0) characters")

                    // Update state with decrypted content
                    await MainActor.run { [decryptedContent] in
                        print("📄 CredentialDetailViewModel: Updating state with decrypted content")
                        self.state.credential = credentialDetail
                        self.state.decryptedContent = decryptedContent
                        self.state.isLoading = false
                        print("✅ CredentialDetailViewModel: Load complete, isLoading = false")
                    }
                }

                // If requireEnclave deferred the action, reset loading state
//                await MainActor.run {
//                    if self.state.decryptedContent == nil {
//                        print("📄 CredentialDetailViewModel: Action was deferred, resetting isLoading")
//                        self.state.isLoading = false
//                    }
//                }
            } catch {
                print("❌ CredentialDetailViewModel: Load failed - \(error.localizedDescription)")
                await MainActor.run {
                    self.state.error = error.localizedDescription
                    self.state.isLoading = false
                    print("📄 CredentialDetailViewModel: Error state set, isLoading = false")
                }
            }
        }
    }

    private func copyToClipboard() {
        guard let content = state.credential?.content else { return }

        UIPasteboard.general.string = content

        state.copySuccess = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) { [weak self] in
            self?.state.copySuccess = false
        }
    }
}
