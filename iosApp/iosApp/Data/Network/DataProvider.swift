import Foundation
import idos_sdk

/// Main data provider for the app that handles all network requests
class DataProvider {
    // MARK: - Properties
    
    private let client: KwilActionClient
    
    // MARK: - Initialization
    
    init(url: String, signer: BaseSigner, chainId: String) {
        self.client = KwilActionClient(baseUrl: url, signer: signer, chainId: chainId)
    }
    
    // MARK: - User Methods
    
    /// Fetches user data from the network
    func getUser() async throws -> GetUserResponse {
        try await withCheckedThrowingContinuation { continuation in
            Task {
                do {
                    let response = try await client.getUser()
                    continuation.resume(returning: response)
                } catch {
                    print("❌ Failed to fetch user: \(error)")
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    /// Checks if a user profile exists for the given address
    func hasUserProfile(address: String) async throws -> Bool {
        try await withCheckedThrowingContinuation { continuation in
            Task {
                do {
                    let response = try await client.hasUserProfile(address: address)
                    continuation.resume(returning: response.hasProfile)
                } catch {
                    print("❌ Failed to check user profile: \(error)")
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    // MARK: - Wallet Methods
    
    /// Fetches all wallets for the current user
    func getWallets() async throws -> [GetWalletsResponse] {
        try await withCheckedThrowingContinuation { continuation in
            Task {
                do {
                    let response = try await client.getWallets()
                    continuation.resume(returning: response)
                } catch {
                    print("❌ Failed to fetch wallets: \(error)")
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    // MARK: - Credential Methods
    
    /// Fetches all credentials for the current user
    func getCredentials() async throws -> [GetCredentialsResponse] {
        try await withCheckedThrowingContinuation { continuation in
            Task {
                do {
                    let response = try await client.getCredentials()
                    continuation.resume(returning: response)
                } catch {
                    print("❌ Failed to fetch credentials: \(error)")
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    /// Fetches a specific credential by its ID
    func getCredential(id: String) async throws -> GetCredentialOwnedResponse {
        try await withCheckedThrowingContinuation { continuation in
            Task {
                do {
                    let response = try await client.getCredentialOwned(id: id)
                    continuation.resume(returning: response)
                } catch {
                    print("❌ Failed to fetch credential \(id): \(error)")
                    continuation.resume(throwing: error)
                }
            }
        }
    }
}
