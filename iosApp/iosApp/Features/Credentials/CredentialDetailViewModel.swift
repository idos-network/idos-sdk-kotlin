import Foundation
import idos_sdk

/// Credential Detail state
struct CredentialDetailState {
    var credential: Credential?
    var isLoading: Bool = false
    var error: String? = nil
    var copySuccess: Bool = false
}

/// Credential Detail events
enum CredentialDetailEvent {
    case loadCredential
    case copyContent
    case clearError
}

/// CredentialDetailViewModel extending BaseEnclaveViewModel
class CredentialDetailViewModel: BaseEnclaveViewModel<CredentialDetailState, CredentialDetailEvent> {
    @Published var state = CredentialDetailState()

    private let credentialId: String
    private let navigationCoordinator: NavigationCoordinator

    init(credentialId: String, enclave: Enclave, navigationCoordinator: NavigationCoordinator) {
        self.credentialId = credentialId
        self.navigationCoordinator = navigationCoordinator
        super.init(enclave: enclave)
        loadCredential()
    }

    func onEvent(_ event: CredentialDetailEvent) {
        switch event {
        case .loadCredential:
            loadCredential()
        case .copyContent:
            copyToClipboard()
        case .clearError:
            state.error = nil
        }
    }

    private func loadCredential() {
        requireEnclave { [weak self] enclave in
            guard let self = self else { return }

            await MainActor.run {
                self.state.isLoading = true
                self.state.error = nil
            }

            // TODO: Fetch credential from API and decrypt using enclave
            // For now, use mock data
            let mockCredential = Credential(
                id: self.credentialId,
                type: "Identity Document",
                issuer: "Government",
                content: """
                {
                  "name": "John Doe",
                  "id": "123456",
                  "birthDate": "1990-01-01",
                  "nationality": "US"
                }
                """
            )

            await MainActor.run {
                self.state.credential = mockCredential
                self.state.isLoading = false
            }
        }
    }

    private func copyToClipboard() {
        guard let content = state.credential?.content else { return }

        #if os(iOS)
        UIPasteboard.general.string = content
        #elseif os(macOS)
        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(content, forType: .string)
        #endif

        state.copySuccess = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) { [weak self] in
            self?.state.copySuccess = false
        }
    }
}