import Foundation
import Combine
import idos_sdk

protocol CredentialsRepositoryProtocol {
    func getCredentials() async throws -> [Credential]
    func getCredential(id: String) async throws -> CredentialDetail
}

class CredentialsRepository: CredentialsRepositoryProtocol {
    private let dataProvider: DataProvider
    
    init(dataProvider: DataProvider) {
        self.dataProvider = dataProvider
    }
    
    func getCredentials() async throws -> [Credential] {
        let response = try await dataProvider.getCredentials()
        return response
            .filter { !$0.publicNotes.isEmpty }
            .map { Credential.from(response: $0) }
    }
    
    func getCredential(id: String) async throws -> CredentialDetail {
        let response = try await dataProvider.getCredential(id: id)
        return CredentialDetail.from(response: response)
    }
}

// MARK: - Mock for Preview and Testing
#if DEBUG
class MockCredentialsRepository: CredentialsRepositoryProtocol {
    func getCredentials() async throws -> [Credential] {
        // Create mock credentials directly
        let credential1 = Credential(
            id: "1",
            type: "KYC",
            level: "L2",
            status: "VERIFIED",
            issuer: "IDOS"
        )
        
        let credential2 = Credential(
            id: "2",
            type: "Identity",
            level: "Basic",
            status: "Active",
            issuer: "Government"
        )
        
        return [credential1, credential2]
    }
    
    func getCredential(id: String) async throws -> CredentialDetail {
        // Create mock credential detail directly
        return CredentialDetail(
            id: id,
            content: "{\"field1\":\"value1\",\"field2\":\"value2\"}",
            encryptorPublicKey: "mock_public_key_\(id)"
        )
    }
}
#endif
