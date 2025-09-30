import Foundation

/**
 * CryptoBridge provides a simplified interface for the KMM framework
 *
 * This acts as a bridge between Swift (CryptoHelperImpl) and Kotlin/Native.
 * The functions are designed to be easily callable from Kotlin via cinterop.
 *
 * All functions return optional Data to indicate success/failure.
 */
@objc public class CryptoBridge: NSObject {

    // MARK: - SCrypt

    /**
     * Perform SCrypt key derivation
     * Returns derived key or nil on failure
     */
    @objc public static func scrypt(
        password: Data,
        salt: Data,
        n: Int,
        r: Int,
        p: Int,
        dkLen: Int
    ) -> Data? {
        return CryptoHelperImpl.scrypt(
            password: password,
            salt: salt,
            n: n,
            r: r,
            p: p,
            dkLen: dkLen
        )
    }

    // MARK: - NaCl Box

    /**
     * Derive public key from secret key
     */
    @objc public static func derivePublicKey(secretKey: Data) -> Data? {
        return CryptoHelperImpl.derivePublicKey(secretKey: secretKey)
    }

    /**
     * Encrypt using NaCl Box
     */
    @objc public static func encryptBox(
        message: Data,
        nonce: Data,
        receiverPublicKey: Data,
        senderSecretKey: Data
    ) -> Data? {
        return CryptoHelperImpl.encryptBox(
            message: message,
            nonce: nonce,
            receiverPublicKey: receiverPublicKey,
            senderSecretKey: senderSecretKey
        )
    }

    /**
     * Decrypt using NaCl Box
     */
    @objc public static func decryptBox(
        ciphertext: Data,
        nonce: Data,
        senderPublicKey: Data,
        receiverSecretKey: Data
    ) -> Data? {
        return CryptoHelperImpl.decryptBox(
            ciphertext: ciphertext,
            nonce: nonce,
            senderPublicKey: senderPublicKey,
            receiverSecretKey: receiverSecretKey
        )
    }

    // MARK: - Utilities

    /**
     * Generate random bytes
     */
    @objc public static func randomBytes(count: Int) -> Data {
        return CryptoHelperImpl.randomBytes(count: count)
    }

    /**
     * Verify crypto operations are working
     */
    @objc public static func verifyCrypto() -> Bool {
        return CryptoHelperImpl.verifyCrypto()
    }
}

/**
 * Singleton instance for easy access from Kotlin
 */
@objc public class CryptoBridgeShared: NSObject {
    @objc public static let shared = CryptoBridgeShared()

    private override init() {
        super.init()
    }

    @objc public func scrypt(
        password: Data,
        salt: Data,
        n: Int,
        r: Int,
        p: Int,
        dkLen: Int
    ) -> Data? {
        return CryptoBridge.scrypt(
            password: password,
            salt: salt,
            n: n,
            r: r,
            p: p,
            dkLen: dkLen
        )
    }

    @objc public func derivePublicKey(secretKey: Data) -> Data? {
        return CryptoBridge.derivePublicKey(secretKey: secretKey)
    }

    @objc public func encryptBox(
        message: Data,
        nonce: Data,
        receiverPublicKey: Data,
        senderSecretKey: Data
    ) -> Data? {
        return CryptoBridge.encryptBox(
            message: message,
            nonce: nonce,
            receiverPublicKey: receiverPublicKey,
            senderSecretKey: senderSecretKey
        )
    }

    @objc public func decryptBox(
        ciphertext: Data,
        nonce: Data,
        senderPublicKey: Data,
        receiverSecretKey: Data
    ) -> Data? {
        return CryptoBridge.decryptBox(
            ciphertext: ciphertext,
            nonce: nonce,
            senderPublicKey: senderPublicKey,
            receiverSecretKey: receiverSecretKey
        )
    }

    @objc public func randomBytes(count: Int) -> Data {
        return CryptoBridge.randomBytes(count: count)
    }

    @objc public func verifyCrypto() -> Bool {
        return CryptoBridge.verifyCrypto()
    }
}