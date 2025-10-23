import Foundation
import idos_sdk

// MARK: - Swift Error for Enclave Operations

/// Swift-friendly error type for enclave operations
/// Maps all Kotlin EnclaveError types to LocalizedError
enum EnclaveSwiftError: LocalizedError {
    case noKey
    case keyExpired
    case decryptionFailed(reason: String)
    case encryptionFailed(String)
    case keyGenerationFailed(String)
    case storageFailed(String)
    case invalidPublicKey(String)
    case unknown(String)

    /// Map Kotlin EnclaveError to Swift error
    static func from(_ kotlinError: Error) -> EnclaveSwiftError {
        // Try to cast to specific EnclaveError types
        if kotlinError is EnclaveErrorNoKey {
            return .noKey
        } else if kotlinError is EnclaveErrorKeyExpired {
            return .keyExpired
        } else if let error = kotlinError as? EnclaveErrorDecryptionFailed {
            let reason = error.reason
            let reasonString: String
            if reason is DecryptFailureWrongPassword {
                reasonString = "Wrong password - key cannot decrypt this data"
            } else if reason is DecryptFailureCorruptedData {
                reasonString = "Encrypted data is corrupted or invalid"
            } else if reason is DecryptFailureInvalidCiphertext {
                reasonString = "Invalid ciphertext format"
            } else {
                reasonString = error.message ?? "Decryption failed"
            }
            return .decryptionFailed(reason: reasonString)
        } else if let error = kotlinError as? EnclaveErrorEncryptionFailed {
            return .encryptionFailed(error.details)
        } else if let error = kotlinError as? EnclaveErrorKeyGenerationFailed {
            return .keyGenerationFailed(error.details)
        } else if let error = kotlinError as? EnclaveErrorStorageFailed {
            return .storageFailed(error.details)
        } else if let error = kotlinError as? EnclaveErrorInvalidPublicKey {
            return .invalidPublicKey(error.details)
        } else {
            let message = (kotlinError as? KotlinThrowable)?.message ?? "Unknown error"
            return .unknown(message ?? "Unknown enclave error")
        }
    }

    var errorDescription: String? {
        switch self {
        case .noKey:
            return "No encryption key present - unlock enclave first"
        case .keyExpired:
            return "Encryption key expired - unlock again"
        case .decryptionFailed(let reason):
            return "Decryption failed: \(reason)"
        case .encryptionFailed(let details):
            return "Encryption failed: \(details)"
        case .keyGenerationFailed(let details):
            return "Key generation failed: \(details)"
        case .storageFailed(let details):
            return "Storage operation failed: \(details)"
        case .invalidPublicKey(let details):
            return "Invalid public key: \(details)"
        case .unknown(let message):
            return "Enclave error: \(message)"
        }
    }
}

// MARK: - Kotlin Result Extensions

extension Kotlin_Result {
    /// Convert Kotlin Result<T> to Swift Result<T, EnclaveSwiftError>
    func toSwift<T>() -> Swift.Result<T, EnclaveSwiftError> {
        if let value = getOrNull() as? T {
            return .success(value)
        } else if let error = exceptionOrNull() {
            return .failure(EnclaveSwiftError.from(error))
        } else {
            return .failure(.unknown("Result conversion failed"))
        }
    }

    /// Convert Kotlin Result<Unit> to Swift Result<Void, EnclaveSwiftError>
    func toSwiftVoid() -> Swift.Result<Void, EnclaveSwiftError> {
        if isSuccess {
            return .success(())
        } else if let error = exceptionOrNull() {
            return .failure(EnclaveSwiftError.from(error))
        } else {
            return .failure(.unknown("Result conversion failed"))
        }
    }
}

// MARK: - Async Helpers for Kotlin Suspend Functions

extension EnclaveOrchestrator {
    /// Check enclave status (async wrapper)
    func checkStatusAsync() async {
        return await withCheckedContinuation { continuation in
            Task {
                do {
                    try await self.checkStatus()
                    continuation.resume()
                } catch {
                    // checkStatus doesn't throw, this is just a safety wrapper
                    continuation.resume()
                }
            }
        }
    }

    /// Unlock enclave (async wrapper with Swift Result)
    func unlockAsync(
        userId: String,
        password: String,
        expirationMillis: Int64
    ) async -> Swift.Result<Void, EnclaveSwiftError> {
        return await withCheckedContinuation { continuation in
            Task {
                do {
                    let result = try await self.unlock(
                        userId: userId,
                        password: password,
                        expirationMillis: expirationMillis
                    )
                    continuation.resume(returning: result.toSwiftVoid())
                } catch {
                    continuation.resume(returning: .failure(.unknown("Unlock failed: \(error.localizedDescription)")))
                }
            }
        }
    }

    /// Lock enclave (async wrapper with Swift Result)
    func lockAsync() async -> Swift.Result<Void, EnclaveSwiftError> {
        return await withCheckedContinuation { continuation in
            Task {
                do {
                    let result = try await self.lock()
                    continuation.resume(returning: result.toSwiftVoid())
                } catch {
                    continuation.resume(returning: .failure(.unknown("Lock failed: \(error.localizedDescription)")))
                }
            }
        }
    }
}

// MARK: - Data / KotlinByteArray Helpers

extension Data {
    /// Convert Swift Data to Kotlin ByteArray
    func toKotlinByteArray() -> KotlinByteArray {
        let nsData = self as NSData
        let bytes = nsData.bytes.bindMemory(to: Int8.self, capacity: self.count)
        return KotlinByteArray(size: Int32(self.count)) { index in
            return KotlinByte(value: bytes[Int(truncating: index)])
        }
    }
}

extension KotlinByteArray {
    /// Convert Kotlin ByteArray to Swift Data
    func toData() -> Data {
        var data = Data(count: Int(self.size))
        data.withUnsafeMutableBytes { buffer in
            for i in 0..<Int(self.size) {
                buffer[i] = UInt8(bitPattern: self.get(index: Int32(i)).int8Value)
            }
        }
        return data
    }
}
