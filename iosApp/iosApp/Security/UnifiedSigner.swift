//
// UnifiedSigner.swift
// iosApp
//
// Unified signer that supports both local (mnemonic-based) and remote (WalletConnect) signing
// Uses protocol-based composition to delegate to either LocalSigner or RemoteSigner
//

import Foundation
import idos_sdk
import os.log

/// Signer type enumeration
enum SignerType {
    case local
    case remote
}

/// Unified signer that delegates to either local or remote signer
class UnifiedSigner: idos_sdk.EthSigner {
    private let storageManager: StorageManager
    private let keyManager: KeyManager
    private let reownWalletManager: ReownWalletManager
    private let logger = Logger(subsystem: "org.idos.app", category: "UnifiedSigner")

    private var localSigner: LocalSigner?
    private var remoteSigner: RemoteSigner?

    init(
        storageManager: StorageManager,
        keyManager: KeyManager,
        reownWalletManager: ReownWalletManager,
        hasher: Keccak256Hasher
    ) {
        self.storageManager = storageManager
        self.keyManager = keyManager
        self.reownWalletManager = reownWalletManager
        super.init(keccak256: hasher)
    }

    // MARK: - EthSigner Override Methods

    override func getIdentifier() -> String {
        guard let address = storageManager.getStoredWallet() else {
            fatalError("No wallet address stored")
        }
        return address.removingPrefix("0x")
    }

    override func __sign(msg: KotlinByteArray) async throws -> KotlinByteArray {
        if let local = localSigner {
            return try await local.__sign(msg: msg)
        } else if let remote = remoteSigner {
            return try await remote.__sign(msg: msg)
        } else {
            throw UnifiedSignerError.noActiveSigner
        }
    }

    override func __signTypedData(typedData: TypedData) async throws -> String {
        if let local = localSigner {
            return try await local.__signTypedData(typedData: typedData)
        } else if let remote = remoteSigner {
            return try await remote.__signTypedData(typedData: typedData)
        } else {
            throw UnifiedSignerError.noActiveSigner
        }
    }

    // MARK: - Signer Management

    /// Activate local signer (mnemonic-based)
    func activateLocalSigner() {
        logger.debug("Activating local signer")
        localSigner = LocalSigner(
            keyManager: keyManager,
            storageManager: storageManager,
            hasher: keccak256
        )
        remoteSigner = nil
    }

    /// Activate remote signer (WalletConnect)
    func activateRemoteSigner() {
        logger.debug("Activating remote signer")
        remoteSigner = RemoteSigner(
            walletManager: reownWalletManager,
            hasher: keccak256
        )
        localSigner = nil
    }

    /// Get the currently active wallet address
    func getActiveAddress() throws -> String {
        if let local = localSigner {
            return local.getActiveAddress()
        } else if let remote = remoteSigner {
            return remote.getActiveAddress()
        } else {
            throw UnifiedSignerError.noActiveSigner
        }
    }

    /// Disconnect and clear all signers
    func disconnect() async throws {
        logger.debug("Disconnecting unified signer")

        // Clear local signer
        if let local = localSigner {
            try local.disconnect()
        }

        // Clear remote signer
        if let remote = remoteSigner {
            try await remote.disconnect()
        }

        // Clear user profile
        storageManager.clearUserProfile()

        // Clear references
        localSigner = nil
        remoteSigner = nil

        logger.debug("Unified signer disconnected")
    }

    /// Get the current signer type
    var currentSignerType: SignerType? {
        if localSigner != nil {
            return .local
        } else if remoteSigner != nil {
            return .remote
        } else {
            return nil
        }
    }
}

// MARK: - Errors
enum UnifiedSignerError: LocalizedError {
    case noActiveSigner

    var errorDescription: String? {
        switch self {
        case .noActiveSigner:
            return "No active signer. Please activate local or remote signer first."
        }
    }
}
