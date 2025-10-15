package org.idos.kwil.domain

import org.idos.kwil.protocol.KwilProtocol
import org.idos.kwil.protocol.callAction
import org.idos.kwil.protocol.executeAction
import org.idos.signer.Signer
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
class ActionExecutor constructor(
    baseUrl: String,
    chainId: String,
    @PublishedApi internal val signer: Signer,
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
    ): List<O> =
        runCatchingAuth {
            client.callAction(action, input, signer)
        }

    /**
     * Executes a view action with no parameters.
     *
     * @param action The no-params view action descriptor
     * @return Result containing list of result rows or error
     */
    suspend inline fun <reified O> call(action: NoParamsAction<O>): List<O> =
        runCatchingAuth {
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
    ): O = call(action, input).firstOrNull() ?: throw DomainError.NotFound(action.name)

    /**
     * Executes a no-params view action and returns a single result.
     *
     * Returns an error if the action returns 0 or more than 1 row.
     *
     * @param action The no-params view action descriptor
     * @return Result containing single result or error
     */
    suspend inline fun <reified O> callSingle(action: NoParamsAction<O>): O =
        call(action).firstOrNull() ?: throw DomainError.NotFound(action.name)

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
    ): HexString = execute(action, listOf(input), synchronous)

    suspend fun <I> execute(
        action: ExecuteAction<I>,
        input: List<I>,
        synchronous: Boolean = true,
    ): HexString =
        runCatchingAuth {
            client.executeAction(action, input, signer, synchronous)
        }

    /**
     * Wraps a suspending block with exception handling and automatic authentication retry.
     *
     * @param block The suspending block to execute
     * @return T containing the block's return value or error
     */
    @PublishedApi
    internal suspend inline fun <T> runCatchingAuth(crossinline block: suspend () -> T): T =
        try {
            runCatchingDomainErrorAsync { block() }
        } catch (e: DomainError.AuthenticationRequired) {
            // Auto-authenticate and retry once
            runCatchingDomainErrorAsync {
                client.authenticate(signer)
                block()
            }
        } catch (e: Exception) {
            // Already a DomainError, pass through
            throw e
        }
}
