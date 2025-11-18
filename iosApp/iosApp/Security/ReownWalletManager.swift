//
// ReownWalletManager.swift
// iosApp
//
// Manager for orchestrating Reown AppKit operations.
// Provides high-level methods for wallet connection and signing.
// Based on AppKitLab example from reown-swift
//

import Foundation
import Combine
import ReownAppKit
import idos_sdk
import os.log

/// Manager for Reown AppKit operations
class ReownWalletManager {
    private let logger = Logger(subsystem: "org.idos.app", category: "ReownWalletManager")
    private let projectId: String
    private var isInitialized = false
    private var cancellables = Set<AnyCancellable>()
    private var pendingPersonalSignContinuation: CheckedContinuation<String, Error>?

    init(projectId: String) {
        self.projectId = projectId
    }

    /// Initialize Reown SDK (AppKit).
    /// Must be called before any other operations.
    func initialize() {
        guard !isInitialized else {
            logger.debug("ReownWalletManager already initialized")
            return
        }

        let metadata = AppMetadata(
            name: "idOS SDK",
            description: "",
            url: "https://idos.network",
            icons: ["https://idos.network/icon.png"],
            redirect: try! AppMetadata.Redirect(native: "idos-app://", universal: nil)
        )

        // Configure Networking first (required by AppKit)
        // Use app group identifier (must start with "group.")
        Networking.configure(
            groupIdentifier: "group.org.idos.app",
            projectId: projectId,
            socketFactory: DefaultSocketFactory()
        )

        // Configure AppKit with proper parameters
        AppKit.configure(
            projectId: projectId,
            metadata: metadata,
            crypto: DefaultCryptoProvider(),
            authRequestParams: nil  // Set to .stub() if SIWE authentication is needed
        ) { error in
            self.logger.error("AppKit configuration error: \(error)")
        }

        // Subscribe to session responses
        AppKit.instance.sessionResponsePublisher
            .sink { [weak self] response in
                self?.handleSessionResponse(response)
            }
            .store(in: &cancellables)

        // Subscribe to socket connection status
        AppKit.instance.socketConnectionStatusPublisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] status in
                self?.logger.debug("Socket connection status: \(String(describing: status))")
            }
            .store(in: &cancellables)

        // Set logging level
        #if DEBUG
        AppKit.instance.logger.setLogging(level: .debug)
        #else
        AppKit.instance.logger.setLogging(level: .error)
        #endif

        // Initialize delegate for additional event handling
        ReownDelegate.shared.initialize()

        isInitialized = true
        logger.debug("ReownWalletManager initialized")
    }

    /// Get the currently connected wallet address
    func getConnectedAddress() -> String? {
        return AppKit.instance.getAddress()
    }

    /// Send a personal_sign request to the connected wallet
    ///
    /// - Parameter message: Message to sign (raw bytes)
    /// - Returns: Signature as hex string
    func personalSign(message: Data) async throws -> String {
        guard let address = getConnectedAddress() else {
            throw WalletError.notConnected
        }

        let messageString = String(data: message, encoding: .utf8) ?? message.hexString

        // Create a continuation to wait for the response
        return try await withCheckedThrowingContinuation { continuation in
            // Store continuation to be resumed when response arrives
            pendingPersonalSignContinuation = continuation

            // Send the request (returns Void)
            Task {
                do {
                    try await AppKit.instance.request(
                        .personal_sign(address: address, message: messageString)
                    )
                } catch {
                    pendingPersonalSignContinuation = nil
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    /// Send an eth_signTypedData_v4 request to the connected wallet
    ///
    /// - Parameter typedData: EIP-712 typed data to sign
    /// - Returns: Signature as hex string
    func signTypedData(_ typedData: TypedData) async throws -> String {
        guard let address = getConnectedAddress() else {
            throw WalletError.notConnected
        }

        // Use TypedData's built-in JSON serialization
        let typedDataJsonString = typedData.toJsonString()

        // Note: W3MJSONRPC doesn't have eth_signTypedData_v4
        // We'll need to use personal_sign as a fallback or wait for SDK support
        // For now, throw an error indicating this isn't supported yet
        logger.error("eth_signTypedData_v4 not yet supported in iOS AppKit")
        throw WalletError.requestFailed("eth_signTypedData_v4 not supported - SDK limitation")
    }

    /// Disconnect from the current wallet session
    func disconnect() async throws {
        // TODO: Check AppKit disconnect API - may need session topic
        // try await AppKit.instance.disconnect(topic: selectedTopic)
        logger.debug("Disconnect not yet implemented")
    }

    // MARK: - Private Methods

    private func handleSessionResponse(_ response: W3MResponse) {
        switch response.result {
        case .response(let value):
            logger.debug("Session response received: \(value.stringRepresentation)")

            // Resume pending personal_sign continuation if exists
            if let continuation = pendingPersonalSignContinuation {
                pendingPersonalSignContinuation = nil
                if let stringValue = value.value as? String {
                    continuation.resume(returning: stringValue)
                } else {
                    continuation.resume(throwing: WalletError.requestFailed("Invalid response type"))
                }
            }

            // Also notify delegate for other listeners
            if let stringValue = value.value as? String {
                ReownDelegate.shared.handleRequestResponse(result: stringValue)
            }
        case .error(let error):
            logger.error("Session error: \(error.message)")

            // Resume pending continuation with error if exists
            if let continuation = pendingPersonalSignContinuation {
                pendingPersonalSignContinuation = nil
                continuation.resume(throwing: WalletError.requestFailed(error.message))
            }

            // Also notify delegate
            ReownDelegate.shared.handleRequestError(WalletError.requestFailed(error.message))
        }
    }
}

// MARK: - WalletError
enum WalletError: LocalizedError {
    case notConnected
    case requestFailed(String)
    case requestExpired
    case sendFailed(String)
    case serializationFailed

    var errorDescription: String? {
        switch self {
        case .notConnected:
            return "Wallet not connected"
        case .requestFailed(let message):
            return "Request failed: \(message)"
        case .requestExpired:
            return "Request expired"
        case .sendFailed(let message):
            return "Failed to send request: \(message)"
        case .serializationFailed:
            return "Failed to serialize data"
        }
    }
}

// MARK: - Data Extension
private extension Data {
    var hexString: String {
        map { String(format: "%02x", $0) }.joined()
    }
}

