import Foundation
import Combine
import idos_sdk

/// Credential model (simplified)
struct Credential: Identifiable {
    let id: String
    let type: String
    let issuer: String
    let content: String
}

/// Credentials state matching Android's CredentialsState
struct CredentialsState {
    var credentials: [Credential] = []
    var isLoading: Bool = false
    var error: String? = nil
}

/// Credentials events matching Android's CredentialsEvent
enum CredentialsEvent {
    case loadCredentials
    case credentialSelected(Credential)
    case clearError
    case refresh
}

/// CredentialsViewModel extending BaseEnclaveViewModel
class CredentialsViewModel: BaseEnclaveViewModel<CredentialsState, CredentialsEvent> {
    @Published var state = CredentialsState()

    private let navigationCoordinator: NavigationCoordinator

    init(enclave: Enclave, navigationCoordinator: NavigationCoordinator) {
        self.navigationCoordinator = navigationCoordinator
        super.init(enclave: enclave)
        loadCredentials()
    }

    func onEvent(_ event: CredentialsEvent) {
        switch event {
        case .loadCredentials, .refresh:
            loadCredentials()
        case .credentialSelected(let credential):
            navigateToDetail(credential)
        case .clearError:
            state.error = nil
        }
    }

    private func loadCredentials() {
        requireEnclave { [weak self] enclave in
            guard let self = self else { return }

            await MainActor.run {
                self.state.isLoading = true
                self.state.error = nil
            }

            // TODO: Fetch credentials from API
            // For now, use mock data
            let mockCredentials = [
                Credential(
                    id: "1",
                    type: "Identity Document",
                    issuer: "Government",
                    content: "{\"name\": \"John Doe\", \"id\": \"123456\"}"
                ),
                Credential(
                    id: "2",
                    type: "Education Certificate",
                    issuer: "University",
                    content: "{\"degree\": \"Bachelor of Science\", \"year\": \"2020\"}"
                )
            ]

            await MainActor.run {
                self.state.credentials = mockCredentials
                self.state.isLoading = false
            }
        }
    }

    private func navigateToDetail(_ credential: Credential) {
        navigationCoordinator.navigate(to: .credentialDetail(credentialId: credential.id))
    }
}