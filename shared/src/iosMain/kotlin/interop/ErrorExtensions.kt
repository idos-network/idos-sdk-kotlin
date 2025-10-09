package org.idos.interop

import org.idos.enclave.EnclaveError
import org.idos.kwil.domain.DomainError
import platform.Foundation.NSError

/**
 * iOS interop extensions for extracting Kotlin errors from Swift NSError.
 *
 * SKIE automatically wraps Kotlin exceptions in NSError when crossing the
 * Swift/Kotlin boundary. These extensions help extract the original Kotlin
 * exception types for proper error handling in Swift.
 *
 * Usage in Swift:
 * ```swift
 * do {
 *     try await client.wallets.add(params)
 * } catch let error as NSError {
 *     if let domainError = error.asDomainError() {
 *         switch onEnum(of: domainError) {
 *         case .validationError(let e):
 *             print("Validation failed: \(e.message)")
 *         case .authenticationRequired(let e):
 *             print("Auth required: \(e.message)")
 *         case .notFound(let e):
 *             print("Not found: \(e.message)")
 *         default:
 *             print("Error: \(domainError.message)")
 *         }
 *     }
 * }
 *
 * // Enclave errors
 * do {
 *     let decrypted = try await orchestrator.decrypt(message: data, senderPublicKey: pubkey)
 * } catch let error as NSError {
 *     if let enclaveError = error.asEnclaveError() {
 *         switch onEnum(of: enclaveError) {
 *         case .noKey:
 *             // Show password prompt
 *         case .keyExpired:
 *             // Regenerate key
 *         case .decryptionFailed(let e):
 *             print("Decryption failed: \(e.reason)")
 *         default:
 *             print("Enclave error: \(enclaveError.message)")
 *         }
 *     }
 * }
 * ```
 */

/**
 * Extracts the original Kotlin exception from an NSError.
 *
 * SKIE stores the Kotlin exception in the NSError's userInfo dictionary
 * under the key "KotlinException".
 *
 * @return The Kotlin exception if present, null otherwise
 */
inline fun <reified T : Throwable> NSError.asKotlinThrowable(): T? =
    this.userInfo["KotlinException"] as? T

/**
 * Extracts a DomainError from an NSError.
 *
 * Use this to handle idOS SDK domain errors (validation, authentication, etc.)
 * from IdosClient operations.
 *
 * @return The DomainError if this NSError wraps one, null otherwise
 */
fun NSError.asDomainError(): DomainError? = asKotlinThrowable()

/**
 * Extracts an EnclaveError from an NSError.
 *
 * Use this to handle encryption/decryption errors from Enclave operations.
 *
 * @return The EnclaveError if this NSError wraps one, null otherwise
 */
fun NSError.asEnclaveError(): EnclaveError? = asKotlinThrowable()
