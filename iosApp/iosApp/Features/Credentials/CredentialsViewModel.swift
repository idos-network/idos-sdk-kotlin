import Foundation
import Combine
import idos_sdk

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

/// CredentialsViewModel matching Android's CredentialsViewModel
class CredentialsViewModel: BaseViewModel<CredentialsState, CredentialsEvent> {
    private let credentialsRepository: CredentialsRepositoryProtocol
    private let navigationCoordinator: NavigationCoordinator

    init(
        credentialsRepository: CredentialsRepositoryProtocol,
        navigationCoordinator: NavigationCoordinator
    ) {
        self.credentialsRepository = credentialsRepository
        self.navigationCoordinator = navigationCoordinator
        super.init(initialState: CredentialsState())
        loadCredentials()
    }

    override func onEvent(_ event: CredentialsEvent) {
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
        Task { [weak self] in
            guard let self = self else { return }

            await MainActor.run {
                self.state.isLoading = true
                self.state.error = nil
            }

            do {
                let credentials = try await credentialsRepository.getCredentials()
                await MainActor.run {
                    self.state.credentials = credentials
                    self.state.isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.state.error = "Failed to load credentials: \(error.localizedDescription)"
                    self.state.isLoading = false
                }
            }
        }
    }

    private func navigateToDetail(_ credential: Credential) {
        navigationCoordinator.navigate(to: .credentialDetail(credentialId: credential.id))
    }
}
