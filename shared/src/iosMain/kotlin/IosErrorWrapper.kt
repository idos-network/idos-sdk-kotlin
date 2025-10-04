package org.idos

import org.idos.kwil.domain.DomainError
import org.idos.kwil.protocol.ProtocolError
import org.idos.kwil.transport.TransportError

/**
 * iOS-compatible error wrapper to handle type erasure for SDK exceptions.
 *
 * Since iOS has type erasure for errors in Result types, this sealed class
 * provides a type-safe way to pattern match on errors in Swift/Obj-C.
 *
 * Usage in Swift:
 * ```swift
 * client.wallets.add(input) { result in
 *     switch result {
 *     case .success(let txHash):
 *         print("Success: \(txHash)")
 *     case .failure(let error):
 *         switch error {
 *         case let domainError as IosKwilError.Domain:
 *             print("Domain error: \(domainError.message)")
 *         case let transportError as IosKwilError.Transport:
 *             print("Network error: \(transportError.message)")
 *         default:
 *             print("Unknown error: \(error.message)")
 *         }
 *     }
 * }
 * ```
 */
sealed class IosKwilError(
    open val message: String,
) {
    /**
     * Domain-level errors (business logic, validation, etc.)
     */
    data class Domain(
        val type: String,
        override val message: String,
        val txHash: String? = null,
    ) : IosKwilError(message)

    /**
     * Protocol-level errors (KWIL RPC issues)
     */
    data class Protocol(
        val code: Int? = null,
        override val message: String,
    ) : IosKwilError(message)

    /**
     * Transport-level errors (network, HTTP, etc.)
     */
    data class Transport(
        val statusCode: Int? = null,
        override val message: String,
    ) : IosKwilError(message)

    /**
     * Unknown or unexpected errors
     */
    data class Unknown(
        override val message: String,
    ) : IosKwilError(message)
}

/**
 * Converts SDK exceptions to iOS-compatible IosKwilError.
 *
 * This function is used internally by the SDK to convert exceptions
 * to a format that Swift can pattern match on.
 */
fun Throwable.toIosError(): IosKwilError =
    when (this) {
        is DomainError.ActionFailed ->
            IosKwilError.Domain(
                type = "ActionFailed",
                message = message ?: "Action execution failed",
                txHash = txHash?.value,
            )

        is DomainError.ValidationError ->
            IosKwilError.Domain(
                type = "ValidationError",
                message = message ?: "Validation failed",
                txHash = null,
            )

        is DomainError.NotFound ->
            IosKwilError.Domain(
                type = "NotFound",
                message = message ?: "Resource not found",
                txHash = null,
            )

        is DomainError.AuthenticationRequired ->
            IosKwilError.Domain(
                type = "AuthenticationRequired",
                message = message ?: "Authentication required",
                txHash = null,
            )

        is DomainError.Unknown ->
            IosKwilError.Unknown(
                message = message ?: "Unknown error",
            )

        is ProtocolError.RpcError ->
            IosKwilError.Protocol(
                code = code,
                message = message ?: "RPC error",
            )

        is ProtocolError.InvalidResponse ->
            IosKwilError.Protocol(
                code = null,
                message = message ?: "Invalid response",
            )

        is ProtocolError.TransactionFailed ->
            IosKwilError.Protocol(
                code = null,
                message = message ?: "Transaction failed",
            )

        is ProtocolError.AuthenticationFailed ->
            IosKwilError.Protocol(
                code = null,
                message = message ?: "Authentication failed",
            )

        is TransportError.NetworkError ->
            IosKwilError.Transport(
                statusCode = null,
                message = message ?: "Network error",
            )

        is TransportError.Timeout ->
            IosKwilError.Transport(
                statusCode = null,
                message = message ?: "Request timeout",
            )

        is TransportError.HttpError ->
            IosKwilError.Transport(
                statusCode = statusCode,
                message = message ?: "HTTP error",
            )

        is TransportError.SerializationError ->
            IosKwilError.Transport(
                statusCode = null,
                message = message ?: "Serialization error",
            )

        else ->
            IosKwilError.Unknown(
                message = message ?: "Unknown error",
            )
    }
