import Foundation
import idos_sdk

/// Main data provider for the app that handles all network requests
class DataProvider {
    // MARK: - Properties
    
    private let client: IdosClient
    
    // MARK: - Initialization
    
    init(url: String, signer: Signer, chainId: String) {
        do {
            self.client = try IdosClient.companion.create(baseUrl: url, chainId: chainId, signer: signer)
        } catch {
            fatalError("Fatal Error: cannot create api client")
        }
    }
    
    // MARK: - User Methods

    /// Fetches user data from the network
    func getUser() async throws -> GetUserResponse {
        return try await client.users.get()
    }

    /// Checks if a user profile exists for the given address
    func hasUserProfile(address: String) async throws -> Bool {
        return try await client.users.hasProfile(address: address).boolValue
    }

    // MARK: - Wallet Methods

    /// Fetches all wallets for the current user
    func getWallets() async throws -> [GetWalletsResponse] {
        return try await client.wallets.getAll()
    }

    // MARK: - Credential Methods

    /// Fetches all credentials for the current user
    func getCredentials() async throws -> [GetCredentialsResponse] {
        return try await client.credentials.getAll()
    }

    /// Fetches a specific credential by its ID
    func getCredential(id: String) async throws -> GetCredentialOwnedResponse {
        return try await client.credentials.getOwned(id: id)
    }}
