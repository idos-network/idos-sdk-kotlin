package org.idos.kwil.protocol

/**
 * Protocol-level errors from KWIL RPC layer.
 *
 * Internal API - these errors are caught and converted to DomainError
 * at the public API boundary.
 */
internal sealed class ProtocolError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /**
     * JSON-RPC error response from KWIL node.
     *
     * @param code RPC error code (e.g., -901 for KGW authentication required)
     * @param message Error message from the node
     */
    class RpcError(
        val code: Int,
        message: String,
    ) : ProtocolError("RPC Error [$code]: $message")

    /**
     * Invalid or unexpected response from KWIL node.
     *
     * @param message Description of the response issue
     */
    class InvalidResponse(
        message: String,
    ) : ProtocolError(message)

    /**
     * Transaction broadcast succeeded but transaction execution failed.
     *
     * @param message Transaction failure description
     * @param txHash Transaction hash
     */
    class TransactionFailed(
        message: String,
        val txHash: String,
    ) : ProtocolError("Transaction $txHash failed: $message")

    /**
     * Challenge-response authentication failed.
     *
     * @param message Authentication failure reason
     */
    class AuthenticationFailed(
        message: String,
    ) : ProtocolError(message)
}
