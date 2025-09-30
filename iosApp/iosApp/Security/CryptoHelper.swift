import Foundation
import CryptoKit
// TODO: Import Sodium framework once added via SPM
// import Sodium

/**
 * CryptoHelper provides cryptographic operations for the KMM shared module
 *
 * This bridges Swift crypto libraries to Kotlin/Native code.
 * Requires SwiftSodium (or similar) for NaCl Box operations.
 *
 * To integrate:
 * 1. Add SwiftSodium via Swift Package Manager
 * 2. Uncomment Sodium imports
 * 3. Build shared framework with these helpers accessible
 */
@objc public class CryptoHelper: NSObject {

    // MARK: - SCrypt Key Derivation

    /**
     * Perform SCrypt key derivation
     *
     * Parameters:
     * - password: Password bytes
     * - salt: Salt bytes (should be UUID)
     * - n: CPU/memory cost (16384 for v0.1)
     * - r: Block size (8)
     * - p: Parallelization (1)
     * - dkLen: Derived key length (32)
     *
     * Returns: Derived key bytes or nil on failure
     */
    @objc public static func scrypt(
        password: Data,
        salt: Data,
        n: Int,
        r: Int,
        p: Int,
        dkLen: Int
    ) -> Data? {
        // Option 1: Use CryptoSwift (most compatible)
        // TODO: Implement using CryptoSwift.Scrypt
        /*
        do {
            let scrypt = try Scrypt(password: Array(password), salt: Array(salt), dkLen: dkLen, N: n, r: r, p: p)
            let derivedKey = try scrypt.calculate()
            return Data(derivedKey)
        } catch {
            print("SCrypt error: \(error)")
            return nil
        }
        */

        // Option 2: Use OpenSSL (if available)
        // let derivedKey = opensslScrypt(password, salt, n, r, p, dkLen)

        // Option 3: Pure Swift implementation (slower)
        // let derivedKey = swiftScrypt(password, salt, n, r, p, dkLen)

        print("⚠️ SCrypt not yet implemented - add CryptoSwift via SPM")
        return nil
    }

    // MARK: - NaCl Box Encryption

    /**
     * Derive Curve25519 public key from secret key
     *
     * Parameters:
     * - secretKey: 32-byte secret key
     *
     * Returns: 32-byte public key or nil on failure
     */
    @objc public static func derivePublicKey(secretKey: Data) -> Data? {
        guard secretKey.count == 32 else {
            print("❌ Secret key must be 32 bytes")
            return nil
        }

        // Using Sodium library (SwiftSodium)
        // TODO: Uncomment once SwiftSodium is added
        /*
        let sodium = Sodium()
        if let keyPair = sodium.box.keyPair(seed: Bytes(secretKey)) {
            return Data(keyPair.publicKey)
        }
        */

        // Alternative: CryptoKit (iOS 13+) - limited Curve25519 support
        // CryptoKit doesn't directly support NaCl Box, but supports Curve25519
        /*
        do {
            let privateKey = try Curve25519.KeyAgreement.PrivateKey(rawRepresentation: secretKey)
            let publicKey = privateKey.publicKey
            return publicKey.rawRepresentation
        } catch {
            print("❌ Public key derivation error: \(error)")
            return nil
        }
        */

        print("⚠️ Public key derivation not yet implemented - add Sodium via SPM")
        return nil
    }

    /**
     * Encrypt message using NaCl Box (crypto_box_easy)
     *
     * Parameters:
     * - message: Plaintext bytes
     * - nonce: 24-byte nonce
     * - receiverPublicKey: Receiver's 32-byte public key
     * - senderSecretKey: Sender's 32-byte secret key
     *
     * Returns: Ciphertext (with MAC) or nil on failure
     */
    @objc public static func encryptBox(
        message: Data,
        nonce: Data,
        receiverPublicKey: Data,
        senderSecretKey: Data
    ) -> Data? {
        guard nonce.count == 24 else {
            print("❌ Nonce must be 24 bytes")
            return nil
        }
        guard receiverPublicKey.count == 32 else {
            print("❌ Receiver public key must be 32 bytes")
            return nil
        }
        guard senderSecretKey.count == 32 else {
            print("❌ Sender secret key must be 32 bytes")
            return nil
        }

        // Using Sodium library (SwiftSodium)
        // TODO: Uncomment once SwiftSodium is added
        /*
        let sodium = Sodium()

        // Perform Box encryption
        if let ciphertext = sodium.box.seal(
            message: Bytes(message),
            recipientPublicKey: Bytes(receiverPublicKey),
            senderSecretKey: Bytes(senderSecretKey),
            nonce: Bytes(nonce)
        ) {
            // crypto_box_easy includes the MAC in the ciphertext
            return Data(ciphertext)
        }
        */

        print("⚠️ Box encryption not yet implemented - add Sodium via SPM")
        return nil
    }

    /**
     * Decrypt message using NaCl Box (crypto_box_open_easy)
     *
     * Parameters:
     * - ciphertext: Encrypted bytes (includes MAC)
     * - nonce: 24-byte nonce
     * - senderPublicKey: Sender's 32-byte public key
     * - receiverSecretKey: Receiver's 32-byte secret key
     *
     * Returns: Plaintext bytes or nil on failure
     */
    @objc public static func decryptBox(
        ciphertext: Data,
        nonce: Data,
        senderPublicKey: Data,
        receiverSecretKey: Data
    ) -> Data? {
        guard nonce.count == 24 else {
            print("❌ Nonce must be 24 bytes")
            return nil
        }
        guard senderPublicKey.count == 32 else {
            print("❌ Sender public key must be 32 bytes")
            return nil
        }
        guard receiverSecretKey.count == 32 else {
            print("❌ Receiver secret key must be 32 bytes")
            return nil
        }

        // Using Sodium library (SwiftSodium)
        // TODO: Uncomment once SwiftSodium is added
        /*
        let sodium = Sodium()

        // Perform Box decryption
        if let plaintext = sodium.box.open(
            authenticatedCipherText: Bytes(ciphertext),
            senderPublicKey: Bytes(senderPublicKey),
            recipientSecretKey: Bytes(receiverSecretKey),
            nonce: Bytes(nonce)
        ) {
            return Data(plaintext)
        }
        */

        print("⚠️ Box decryption not yet implemented - add Sodium via SPM")
        return nil
    }
}

// MARK: - Helpers

extension CryptoHelper {
    /**
     * Generate secure random bytes
     */
    @objc public static func randomBytes(count: Int) -> Data {
        var bytes = [UInt8](repeating: 0, count: count)
        _ = SecRandomCopyBytes(kSecRandomDefault, count, &bytes)
        return Data(bytes)
    }
}

// MARK: - Type Aliases
typealias Bytes = [UInt8]