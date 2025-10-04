package org.idos.kwil.domain

import org.idos.kwil.types.HexString

/**
 * Domain-level errors exposed in public APIs.
 *
 * These errors represent business logic failures, validation issues,
 * and other application-level problems.
 */
sealed class DomainError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /**
     * Action execution failed on the KWIL network.
     *
     * @param message Error description
     * @param txHash Transaction hash if transaction was broadcasted
     */
    class ActionFailed(
        message: String,
        val txHash: HexString? = null,
    ) : DomainError(message)

    /**
     * Input validation failed before submitting to KWIL.
     *
     * @param message Validation error description
     */
    class ValidationError(
        message: String,
    ) : DomainError(message)

    /**
     * Requested resource not found.
     *
     * @param message Description of what was not found
     */
    class NotFound(
        message: String,
    ) : DomainError(message)

    /**
     * Authentication is required but not provided or invalid.
     *
     * @param message Authentication error description
     */
    class AuthenticationRequired(
        message: String,
    ) : DomainError(message)

    /**
     * Unknown or unexpected error.
     *
     * @param message Error description
     * @param cause Original exception if available
     */
    class Unknown(
        message: String,
        cause: Throwable? = null,
    ) : DomainError(message, cause)
}
