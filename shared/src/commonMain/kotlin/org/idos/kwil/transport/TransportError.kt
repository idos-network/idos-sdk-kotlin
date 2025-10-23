package org.idos.kwil.transport

/**
 * Transport-level errors from HTTP/network layer.
 *
 * Internal API - these errors are caught and converted to DomainError
 * at the public API boundary.
 */
internal sealed class TransportError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /**
     * Network connectivity error.
     *
     * @param message Error description
     * @param cause Original network exception
     */
    class NetworkError(
        message: String,
        cause: Throwable,
    ) : TransportError(message, cause)

    /**
     * HTTP request timeout.
     *
     * @param message Timeout description
     */
    class Timeout(
        message: String,
    ) : TransportError(message)

    /**
     * HTTP error response (4xx, 5xx).
     *
     * @param statusCode HTTP status code
     * @param message Error message
     */
    class HttpError(
        val statusCode: Int,
        message: String,
    ) : TransportError("HTTP $statusCode: $message")

    /**
     * JSON serialization/deserialization error.
     *
     * @param message Serialization error description
     * @param cause Original serialization exception
     */
    class SerializationError(
        message: String,
        cause: Throwable,
    ) : TransportError(message, cause)
}
