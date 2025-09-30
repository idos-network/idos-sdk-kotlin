import Foundation
import Sodium
import CryptoSwift

/**
 * Complete CryptoHelper implementation with Sodium and CryptoSwift
 *
 * This provides full NaCl Box encryption and SCrypt key derivation
 * compatible with the Android libsodium implementation.
 */
@objc public class CryptoHelperImpl: NSObject {

    private static let sodium = Sodium()

    // MARK: - SCrypt Key Derivation

    /**
     * Perform SCrypt key derivation using CryptoSwift
     */
    @objc public static func scrypt(
        password: Data,
        salt: Data,
        n: Int,
        r: Int,
        p: Int,
        dkLen: Int
    ) -> Data? {
        do {
            let passwordBytes = Array(password)
            let saltBytes = Array(salt)

            let derivedKey = try CryptoSwift.Scrypt(
                password: passwordBytes,
                salt: saltBytes,
                dkLen: dkLen,
                N: n,
                r: r,
                p: p
            ).calculate()

            return Data(derivedKey)
        } catch {
            print("❌ SCrypt error: \(error)")
            return nil
        }
    }

    // MARK: - NaCl Box Encryption (Curve25519 + XSalsa20 + Poly1305)

    /**
     * Derive Curve25519 public key from secret key
     * Uses libsodium's crypto_scalarmult_base
     */
    @objc public static func derivePublicKey(secretKey: Data) -> Data? {
        guard secretKey.count == 32 else {
            print("❌ Secret key must be 32 bytes")
            return nil
        }

        // Generate key pair from seed (secret key)
        if let keyPair = sodium.box.keyPair(seed: Bytes(secretKey)) {
            return Data(keyPair.publicKey)
        }

        print("❌ Failed to derive public key")
        return nil
    }

    /**
     * Encrypt message using NaCl Box (crypto_box_easy)
     *
     * This performs authenticated encryption using:
     * - Curve25519 for key exchange
     * - XSalsa20 for encryption
     * - Poly1305 for authentication
     *
     * Output format: MAC (16 bytes) + ciphertext
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

        // Perform Box encryption (crypto_box_easy)
        if let ciphertext = sodium.box.seal(
            message: Bytes(message),
            recipientPublicKey: Bytes(receiverPublicKey),
            senderSecretKey: Bytes(senderSecretKey),
            nonce: Bytes(nonce)
        ) {
            return Data(ciphertext)
        }

        print("❌ Box encryption failed")
        return nil
    }

    /**
     * Decrypt message using NaCl Box (crypto_box_open_easy)
     *
     * Input format: MAC (16 bytes) + ciphertext
     * Returns plaintext or nil on authentication failure
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

        // Perform Box decryption (crypto_box_open_easy)
        if let plaintext = sodium.box.open(
            authenticatedCipherText: Bytes(ciphertext),
            senderPublicKey: Bytes(senderPublicKey),
            recipientSecretKey: Bytes(receiverSecretKey),
            nonce: Bytes(nonce)
        ) {
            return Data(plaintext)
        }

        print("❌ Box decryption failed (authentication error)")
        return nil
    }

    // MARK: - Utility Functions

    /**
     * Generate cryptographically secure random bytes
     */
    @objc public static func randomBytes(count: Int) -> Data {
        guard let bytes = sodium.randomBytes.buf(length: count) else {
            // Fallback to SecRandomCopyBytes
            var fallbackBytes = [UInt8](repeating: 0, count: count)
            _ = SecRandomCopyBytes(kSecRandomDefault, count, &fallbackBytes)
            return Data(fallbackBytes)
        }
        return Data(bytes)
    }

    /**
     * Verify Sodium library is working correctly
     */
    @objc public static func verifyCrypto() -> Bool {
        // Test random bytes generation
        guard let _ = sodium.randomBytes.buf(length: 32) else {
            print("❌ Sodium random bytes failed")
            return false
        }

        // Test key pair generation
        guard let _ = sodium.box.keyPair() else {
            print("❌ Sodium key pair generation failed")
            return false
        }

        // Test SCrypt
        let testPassword = Data("test".utf8)
        let testSalt = Data("testsalt".utf8)
        guard let _ = scrypt(password: testPassword, salt: testSalt, n: 1024, r: 8, p: 1, dkLen: 32) else {
            print("❌ SCrypt failed")
            return false
        }

        print("✅ Crypto verification successful")
        return true
    }
}

// MARK: - Type Aliases
private typealias Bytes = [UInt8]