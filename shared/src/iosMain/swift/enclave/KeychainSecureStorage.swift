import Foundation
import Security

/// Production-ready iOS Keychain implementation of SecureStorage.
///
/// This class is bundled with the SDK via SKIE and provides secure, persistent
/// storage for Enclave encryption keys using iOS Keychain.
///
/// ## Security Features:
/// - Uses `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` for maximum security
/// - Only accessible when device is unlocked
/// - Never backed up to iCloud
/// - Not migrated to new devices
/// - Matches Android's EncryptedFile + StrongBox security level
/// - Supports separate storage for USER and MPC enclave types
///
/// ## Usage:
/// This class is automatically available in Swift when importing the SDK:
/// ```swift
/// import idos_sdk
///
/// let storage = KeychainSecureStorage()
/// let orchestrator = EnclaveOrchestrator.create(
///     encryption: IosEncryption(storage: storage),
///     metadataStorage: IosMetadataStorage()
/// )
/// ```
public class KeychainSecureStorage: SecureStorage {
    private let baseKeyTag = "org.idos.enclave.key"

    public enum KeychainError: Error {
        case storageFailed(OSStatus)
        case retrievalFailed(OSStatus)
        case deletionFailed(OSStatus)
    }

    public init() {}

    /// Generate keychain tag for specific enclave type
    private func keyTag(for type: EnclaveKeyType) -> String {
        return "\(baseKeyTag)_\(type.name)"
    }

    /// Stores an encryption key in iOS Keychain for a specific enclave type.
    ///
    /// Uses:
    /// - `kSecClassKey` for key storage
    /// - `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` for maximum security
    /// - Replaces any existing key for the given enclave type
    ///
    /// - Parameters:
    ///   - key: The encryption key bytes to store (as KotlinByteArray)
    ///   - enclaveKeyType: The enclave type (USER or MPC)
    /// - Throws: KeychainError if storage fails
    public func __storeKey(key: KotlinByteArray, enclaveKeyType: EnclaveKeyType) async throws {
        let data = key.toNSData()
        let tag = keyTag(for: enclaveKeyType)

        // Delete existing key first for this enclave type
        try? await deleteKey(enclaveKeyType: enclaveKeyType)

        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: tag,
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]

        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess else {
            throw KeychainError.storageFailed(status)
        }
    }

    /// Retrieves the stored encryption key from iOS Keychain for a specific enclave type.
    ///
    /// - Parameter enclaveKeyType: The enclave type (USER or MPC)
    /// - Returns: The stored key bytes as KotlinByteArray, or nil if no key is stored
    /// - Throws: KeychainError if retrieval fails (not including "not found")
    public func __retrieveKey(enclaveKeyType: EnclaveKeyType) async throws -> KotlinByteArray? {
        let tag = keyTag(for: enclaveKeyType)

        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: tag,
            kSecReturnData as String: true
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        if status == errSecItemNotFound {
            return nil
        }

        guard status == errSecSuccess, let keyData = result as? Data else {
            throw KeychainError.retrievalFailed(status)
        }

        return keyData.toKotlinByteArray()
    }

    /// Deletes the stored encryption key from iOS Keychain for a specific enclave type.
    ///
    /// Succeeds silently if no key exists.
    ///
    /// - Parameter enclaveKeyType: The enclave type (USER or MPC)
    /// - Throws: KeychainError if deletion fails
    public func __deleteKey(enclaveKeyType: EnclaveKeyType) async throws {
        let tag = keyTag(for: enclaveKeyType)

        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: tag
        ]

        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.deletionFailed(status)
        }
    }
}
