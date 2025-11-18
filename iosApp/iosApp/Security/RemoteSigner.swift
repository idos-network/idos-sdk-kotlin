//
// RemoteSigner.swift
// iosApp
//
// Signer implementation that delegates to external wallet via Reown/WalletConnect
//

import Foundation
import idos_sdk
import os.log

/// Remote signer that uses external wallet for signing operations
class RemoteSigner: idos_sdk.EthSigner {
    private let walletManager: ReownWalletManager
    private let logger = Logger(subsystem: "org.idos.app", category: "RemoteSigner")

    init(walletManager: ReownWalletManager, hasher: Keccak256Hasher) {
        self.walletManager = walletManager
        super.init(keccak256: hasher)
    }

    // MARK: - EthSigner Override Methods

    /// Get Ethereum address identifier
    override func getIdentifier() -> String {
        guard let address = walletManager.getConnectedAddress() else {
            fatalError("No wallet connected")
        }
        return address.removingPrefix("0x")
    }

    /// Sign message using external wallet via WalletConnect
    ///
    /// - Parameter msg: Message bytes to sign as KotlinByteArray
    /// - Returns: 65-byte signature as KotlinByteArray
    override func __sign(msg: KotlinByteArray) async throws -> KotlinByteArray {
        logger.debug("Signing message via external wallet")

        do {
            let message = msg.toNSData()
            let signatureHex = try await walletManager.personalSign(message: message)
            let signature = Data(hex: signatureHex.removingPrefix("0x"))

            logger.debug("Message signed successfully, signature length: \(signature.count)")
            return signature.toKotlinByteArray()
        } catch {
            logger.error("Failed to sign message with external wallet: \(error.localizedDescription)")
            throw error
        }
    }

    /// Sign EIP-712 typed data using external wallet
    ///
    /// - Parameter typedData: EIP-712 TypedData structure
    /// - Returns: Signature as hex string
    override func __signTypedData(typedData: TypedData) async throws -> String {
        logger.debug("Signing typed data via external wallet")

        do {
            let signature = try await walletManager.signTypedData(typedData)
            logger.debug("Typed data signed successfully")
            return signature
        } catch {
            logger.error("Failed to sign typed data with external wallet: \(error.localizedDescription)")
            throw error
        }
    }

    // MARK: - Helper Methods

    /// Get the currently connected wallet address
    func getActiveAddress() -> String {
        guard let address = walletManager.getConnectedAddress() else {
            fatalError("No wallet connected")
        }
        return address
    }

    /// Disconnect from the external wallet
    func disconnect() async throws {
        try await walletManager.disconnect()
    }
}

// MARK: - Data Extension
private extension Data {
    init(hex: String) {
        var data = Data()
        var hex = hex

        // Remove 0x prefix if present
        if hex.hasPrefix("0x") {
            hex = String(hex.dropFirst(2))
        }

        // Convert hex string to Data
        var index = hex.startIndex
        while index < hex.endIndex {
            let nextIndex = hex.index(index, offsetBy: 2, limitedBy: hex.endIndex) ?? hex.endIndex
            let byteString = hex[index..<nextIndex]
            if let byte = UInt8(byteString, radix: 16) {
                data.append(byte)
            }
            index = nextIndex
        }

        self = data
    }
}
