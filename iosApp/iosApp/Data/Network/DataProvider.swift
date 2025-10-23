import Foundation
import OSLog
import idos_sdk

/// Main data provider for the app that handles all network requests
class DataProvider {
    // MARK: - Properties

    private let client: IdosClient

    // MARK: - Initialization

    /// Initialize DataProvider with SDK logging configuration
    /// - Parameters:
    ///   - url: Kwil node URL
    ///   - signer: Ethereum signer for authentication
    ///   - chainId: Kwil chain ID
    ///   - logConfig: SDK logging configuration (provided by DIContainer)
    init(
        url: String,
        signer: Signer,
        chainId: String,
        logConfig: IdosLogConfig
    ) {
        do {
            self.client = try IdosClient.companion.create(
                baseUrl: url,
                chainId: chainId,
                signer: signer,
                logConfig: logConfig
            )
            Logger.network.info("DataProvider: IdosClient initialized successfully")
        } catch {
            Logger.network.fault("DataProvider: Fatal error - cannot create API client: \(error)")
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
