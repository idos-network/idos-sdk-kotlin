import Foundation
import Security

/// KeyManager for iOS using Keychain
/// Matches Android's KeyManager.kt using EncryptedFile + StrongBox
class KeyManager {
    private let keyTag = "org.idos.app.ethkey"

    enum KeyManagerError: Error {
        case keyGenerationFailed
        case keyStorageFailed
        case keyRetrievalFailed
        case keyDeletionFailed
    }
    
    /// Generate and store key from mnemonic phrase
    /// Matches Android's KeyManager.generateAndStoreKey()
    /// - Parameter words: Mnemonic phrase (12 or 24 words)
    /// - Returns: Derived Ethereum address
    /// - Throws: KeyManagerError if generation or storage fails
    func generateAndStoreKey(words: String) throws -> String {
        // Derive private key from mnemonic
        var key = try EthSigner.mnemonicToPrivateKey(words)

        defer {
            // Zero out key data for security (matches Android's key.fill(0))
            key.resetBytes(in: 0..<key.count)
        }

        // Derive address before storing
        let address = try EthSigner.privateKeyToAddress(key)

        // Store the key securely
        try deleteKey()
        try storeKey(key)

        return address
    }

    /// Store a private key securely in Keychain
    func storeKey(_ keyData: Data) throws {
        // Delete any existing key first
        try? deleteKey()

        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: keyTag,
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecValueData as String: keyData,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]

        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess else {
            throw KeyManagerError.keyStorageFailed
        }
    }

    /// Retrieve the private key from Keychain
    func getKey() throws -> Data {
        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: keyTag,
            kSecReturnData as String: true
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess, let keyData = result as? Data else {
            throw KeyManagerError.keyRetrievalFailed
        }

        return keyData
    }

    /// Check if a key exists
    func hasKey() -> Bool {
        do {
            _ = try getKey()
            return true
        } catch {
            return false
        }
    }

    /// Delete the stored key
    func deleteKey() throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: keyTag
        ]

        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeyManagerError.keyDeletionFailed
        }
    }

    /// Store mnemonic-derived key (from BIP39/BIP44)
    func storeMnemonicKey(_ privateKeyHex: String) throws {
        guard let keyData = Data(hexString: privateKeyHex) else {
            throw KeyManagerError.keyGenerationFailed
        }
        try storeKey(keyData)
    }
}

// MARK: - Data Extension for Hex Conversion
extension Data {
    init?(hexString: String) {
        let cleanHex = hexString.replacingOccurrences(of: "0x", with: "")
        guard cleanHex.count % 2 == 0 else { return nil }

        var data = Data(capacity: cleanHex.count / 2)
        var index = cleanHex.startIndex

        while index < cleanHex.endIndex {
            let nextIndex = cleanHex.index(index, offsetBy: 2)
            guard let byte = UInt8(cleanHex[index..<nextIndex], radix: 16) else {
                return nil
            }
            data.append(byte)
            index = nextIndex
        }

        self = data
    }

    func toHexString() -> String {
        return map { String(format: "%02x", $0) }.joined()
    }
}
