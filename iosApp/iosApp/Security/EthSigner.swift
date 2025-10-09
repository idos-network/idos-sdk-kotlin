import Foundation
import WalletCore
import idos_sdk

/// EthSigner provides Ethereum wallet functionality matching Android's EthSigner.kt
/// Handles BIP39 mnemonic derivation, key management, and EIP-191 signing
class EthSigner: idos_sdk.EthSigner {
    private let keyManager: KeyManager
    private let storageManager: StorageManager

    /// Standard Ethereum BIP44 derivation path matching Android
    /// m/44'/60'/0'/0/47
    static let defaultDerivationPath = "m/44'/60'/0'/0/47"

    enum EthSignerError: Error {
        case invalidMnemonic
        case invalidPrivateKey
        case keyDerivationFailed
        case signingFailed
        case noStoredKey
    }

    init(keyManager: KeyManager, storageManager: StorageManager) {
        self.keyManager = keyManager
        self.storageManager = storageManager
    }

    // MARK: - EthSigner Override Methods

    /// Get Ethereum address identifier
    /// Matches Android's EthSigner.getIdentifier()
    override func getIdentifier() -> String {
        let address = storageManager.getStoredWallet()
        guard let address = address else {
            fatalError("Wallet address not found in storage")
        }
        return address.removingPrefix("0x")
    }

    /// Sign message with EIP-191 personal sign
    /// Matches Android's EthSigner.sign()
    ///
    /// - Parameter msg: Message bytes to sign as KotlinByteArray
    /// - Returns: 65-byte signature as KotlinByteArray
    override func __sign(msg: KotlinByteArray) async throws -> KotlinByteArray {
        // Convert KotlinByteArray to Data
        let message = msg.toNSData()

        // Get stored private key
        guard let privateKeyData = try? keyManager.getKey() else {
            throw EthSignerError.noStoredKey
        }

        defer {
            // Clear private key from memory
            var mutableKey = privateKeyData
            mutableKey.withUnsafeMutableBytes { ptr in
                if let baseAddress = ptr.baseAddress {
                    memset(baseAddress, 0, privateKeyData.count)
                }
            }
        }

        guard let privateKey = PrivateKey(data: privateKeyData) else {
            throw EthSignerError.invalidPrivateKey
        }

        // EIP-191 personal sign: "\x19Ethereum Signed Message:\n" + len(message) + message
        let prefix = "\u{19}Ethereum Signed Message:\n\(message.count)"
        var prefixedMessage = Data(prefix.utf8)
        prefixedMessage.append(message)

        // Hash with keccak256
        let hash = Hash.keccak256(data: prefixedMessage)

        // Sign with secp256k1
        guard let signature = privateKey.sign(digest: hash, curve: .secp256k1) else {
            throw EthSignerError.signingFailed
        }

        // Convert signature to KotlinByteArray
        return signature.toKotlinByteArray()
    }

    // MARK: - Static Utility Methods (Companion)

    /// Convert mnemonic phrase to private key bytes using BIP39/BIP44
    /// Matches Android's companion object extension: String.mnemonicToKeypair()
    ///
    /// - Parameters:
    ///   - mnemonic: 12 or 24 word mnemonic phrase
    ///   - derivationPath: BIP44 derivation path (default: m/44'/60'/0'/0/47)
    /// - Returns: 32-byte private key
    static func mnemonicToPrivateKey(_ mnemonic: String, derivationPath: String = defaultDerivationPath) throws -> Data {
        // Clean up mnemonic
        let cleanMnemonic = mnemonic
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .components(separatedBy: .whitespaces)
            .filter { !$0.isEmpty }
            .joined(separator: " ")

        // Validate mnemonic using WalletCore
        guard Mnemonic.isValid(mnemonic: cleanMnemonic) else {
            throw EthSignerError.invalidMnemonic
        }

        // Create HDWallet from mnemonic (BIP39)
        // Note: Using empty passphrase like Android implementation
        guard let wallet = HDWallet(mnemonic: cleanMnemonic, passphrase: "") else {
            throw EthSignerError.invalidMnemonic
        }

        // Derive key using BIP32/BIP44
        // WalletCore uses DerivationPath for path parsing
        guard let derivation = DerivationPath(derivationPath) else {
            throw EthSignerError.keyDerivationFailed
        }

        let privateKey = wallet.getKey(coin: .ethereum, derivationPath: derivation.description)
        return privateKey.data
    }

    /// Derive Ethereum address from private key
    /// Matches Android's companion object extension: ByteArray.privateToAddress()
    ///
    /// - Parameter privateKey: 32-byte private key
    /// - Returns: Ethereum address with 0x prefix
    static func privateKeyToAddress(_ privateKey: Data) throws -> String {
        guard let privKey = PrivateKey(data: privateKey) else {
            throw EthSignerError.invalidPrivateKey
        }

        let publicKey = privKey.getPublicKeySecp256k1(compressed: false)
        let address = AnyAddress(publicKey: publicKey, coin: .ethereum)
        return address.description
    }
}
