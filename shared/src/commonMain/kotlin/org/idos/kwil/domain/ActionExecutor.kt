package org.idos.kwil.domain

import org.idos.kwil.domain.generated.ExecuteAction
import org.idos.kwil.domain.NoParamsAction
import org.idos.kwil.domain.generated.ViewAction
import org.idos.kwil.protocol.KwilProtocol
import org.idos.kwil.protocol.MissingAuthenticationException
import org.idos.kwil.protocol.callAction
import org.idos.kwil.protocol.executeAction
import org.idos.kwil.security.signer.Signer
import org.idos.kwil.types.HexString

/**
 * Executes KWIL schema actions with Result-based error handling for public APIs.
 *
 * This class wraps KwilActionClient and converts all exceptions to Result types,
 * making it safe for iOS consumption where exceptions are not idiomatic.
 *
 * @param baseUrl KWIL network URL
 * @param chainId Chain identifier
 * @param signer Cryptographic signer for transactions
 */
class ActionExecutor internal constructor(
    baseUrl: String,
    chainId: String,
    @PublishedApi
    internal val signer: Signer,
) {
    @PublishedApi
    internal val client = KwilProtocol(baseUrl, chainId)

    /**
     * Executes a view action (read-only query).
     *
     * View actions don't modify state and return data from the database.
     *
     * @param action The view action descriptor
     * @param input Action input parameters
     * @return Result containing list of result rows or error
     */
    suspend inline fun <I, reified O> call(
        action: ViewAction<I, O>,
        input: I,
    ): Result<List<O>> =
        runCatchingError {
            client.callAction(action, input, signer)
        }

    /**
     * Executes a view action with no parameters.
     *
     * @param action The no-params view action descriptor
     * @return Result containing list of result rows or error
     */
    suspend inline fun <reified O> call(action: NoParamsAction<O>): Result<List<O>> =
        runCatchingError {
            client.callAction(action, signer)
        }

    /**
     * Executes a view action and returns a single result.
     *
     * Returns an error if the action returns 0 or more than 1 row.
     * This provides a single point of error handling - both "not found" and other errors
     * are returned as Result.failure.
     *
     * @param action The view action descriptor
     * @param input Action input parameters
     * @return Result containing single result or error
     */
    suspend inline fun <I, reified O> callSingle(
        action: ViewAction<I, O>,
        input: I,
    ): Result<O> = call(action, input).mapCatching { it.single() }

    /**
     * Executes a no-params view action and returns a single result.
     *
     * Returns an error if the action returns 0 or more than 1 row.
     *
     * @param action The no-params view action descriptor
     * @return Result containing single result or error
     */
    suspend inline fun <reified O> callSingle(action: NoParamsAction<O>): Result<O> = call(action).mapCatching { it.single() }

    /**
     * Executes a mutative action (transaction).
     *
     * Execute actions modify database state and must be mined on the blockchain.
     * By default, this waits for transaction commit (synchronous = true).
     *
     * @param action The execute action descriptor
     * @param input Action input parameters
     * @param synchronous If true, waits for transaction commit; if false, returns after broadcast
     * @return Result containing transaction hash or error
     */
    suspend fun <I> execute(
        action: ExecuteAction<I>,
        input: I,
        synchronous: Boolean = true,
    ): Result<HexString> =
        runCatchingError {
            client.executeAction(action, input, signer, synchronous)
        }

    /**
     * Wraps a suspending block with exception handling and automatic authentication retry.
     *
     * Converts all exceptions to appropriate DomainError types:
     * - DomainError → preserved (already domain error)
     * - MissingAuthenticationException → auto-authenticate and retry once
     * - IllegalArgumentException → DomainError.ValidationError
     * - Other exceptions → DomainError.Unknown
     *
     * @param block The suspending block to execute
     * @return Result containing the block's return value or error
     */
    @PublishedApi
    internal suspend inline fun <T> runCatchingError(crossinline block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: DomainError) {
            // Already a DomainError, pass through
            Result.failure(e)
        } catch (e: MissingAuthenticationException) {
            // Auto-authenticate and retry once
            try {
                client.authenticate(signer)
                Result.success(block())
            } catch (retryError: Exception) {
                Result.failure(
                    DomainError.AuthenticationRequired(
                        "Authentication failed: ${retryError.message}",
                    ),
                )
            }
        } catch (e: IllegalArgumentException) {
            Result.failure(
                DomainError.ValidationError(
                    e.message ?: "Invalid input",
                ),
            )
        } catch (e: Exception) {
            Result.failure(
                DomainError.Unknown(
                    e.message ?: "An unexpected error occurred",
                    cause = e,
                ),
            )
        }

    companion object {
        /**
         * Creates a new ActionExecutor instance.
         *
         * @param baseUrl KWIL network URL (e.g., "https://nodes.staging.idos.network")
         * @param chainId Chain identifier
         * @param signer Cryptographic signer for transactions
         * @return Result containing ActionExecutor or error
         */
        fun create(
            baseUrl: String,
            chainId: String,
            signer: Signer,
        ): Result<ActionExecutor> =
            try {
                Result.success(ActionExecutor(baseUrl, chainId, signer))
            } catch (e: Exception) {
                Result.failure(
                    DomainError.Unknown(
                        e.message ?: "Failed to create ActionExecutor",
                        cause = e,
                    ),
                )
            }
    }
}
