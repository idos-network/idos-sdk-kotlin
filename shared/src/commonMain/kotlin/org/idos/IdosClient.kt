package org.idos

import org.idos.kwil.domain.ActionExecutor
import org.idos.kwil.domain.DomainError
import org.idos.kwil.domain.generated.view.GetUserResponse
import org.idos.kwil.domain.runCatchingDomainError
import org.idos.logging.IdosLogConfig
import org.idos.logging.IdosLogger
import org.idos.signer.Signer

/**
 * Main entry point for the idOS SDK.
 *
 * This class provides organized access to idOS operations through object groups.
 * All operations are defined as extensions in IdosClientExtensions.kt for easy
 * discoverability in IDEs.
 *
 * ## Error Handling
 * - All operations are suspend functions that throw [DomainError] on failure
 * - Wrap calls in try-catch to handle errors gracefully
 * - [DomainError] contains detailed information about what went wrong
 *
 * ## Example usage:
 * ```kotlin
 * try {
 *     val client = IdosClient.create(
 *         baseUrl = "https://nodes.staging.idos.network",
 *         chainId = "idos-testnet",
 *         signer = EthSigner(privateKey)
 *     )
 *
 *     // IDE autocomplete shows available object groups
 *     val txHash = client.wallets.add(...)
 *     val credentials = client.credentials.getOwned(id)
 *     client.accessGrants.create(...)
 *     val user = client.users.get()
 * } catch (e: DomainError) {
 *     // Handle domain-specific errors
 *     println("Operation failed: ${e.message}")
 * }
 * ```
 *
 * @see DomainError for error types and handling
 */
class IdosClient internal constructor(
    @PublishedApi internal val executor: ActionExecutor,
    val chainId: String,
) {
    /**
     * Wallet operations group.
     *
     * Extensions in IdosClientExtensions.kt:
     * - add(input): Add a new wallet
     * - getAll(): Get all wallets for current user
     * - remove(id): Remove a wallet
     */
    val wallets = Wallets(this)

    class Wallets internal constructor(
        internal val client: IdosClient,
    )

    /**
     * Credential operations group.
     *
     * Extensions in IdosClientExtensions.kt:
     * - add(input): Add a new credential
     * - getOwned(): Get credentials owned by user
     * - getShared(): Get credentials shared with user
     * - edit(input): Edit a credential
     * - remove(id): Remove a credential
     * - share(input): Share a credential
     */
    val credentials = Credentials(this)

    class Credentials internal constructor(
        internal val client: IdosClient,
    )

    /**
     * Access grant operations group.
     *
     * Extensions in IdosClientExtensions.kt:
     * - create(input): Create an access grant
     * - getOwned(): Get grants owned by user
     * - getGranted(): Get grants granted by user
     * - getForCredential(id): Get grants for specific credential
     * - revoke(input): Revoke an access grant
     */
    val accessGrants = AccessGrants(this)

    class AccessGrants internal constructor(
        internal val client: IdosClient,
    )

    /**
     * User operations group.
     *
     * Extensions in IdosClientExtensions.kt:
     * - get(): Get current user
     * - hasProfile(): Check if user has a profile
     */
    val users = Users(this)

    class Users internal constructor(
        internal val client: IdosClient,
    ) {
        public suspend fun getUser(): GetUserResponse = client.users.get()
    }

    /**
     * Attribute operations group.
     *
     * Extensions in IdosClientExtensions.kt:
     * - add(input): Add an attribute
     * - getAll(): Get all attributes
     * - edit(input): Edit an attribute
     * - remove(id): Remove an attribute
     * - share(input): Share an attribute
     */
    val attributes = Attributes(this)

    class Attributes internal constructor(
        internal val client: IdosClient,
    )

    companion object {
        /**
         * Creates a new IdosClient instance.
         *
         * @param baseUrl KWIL network URL (e.g., "https://nodes.staging.idos.network")
         * @param chainId Chain identifier (e.g., "idos-testnet")
         * @param signer Cryptographic signer for transactions
         * @param logConfig Logging configuration for HTTP and SDK logging
         * @return IdosClient instance
         */
        @Throws(DomainError::class)
        fun create(
            baseUrl: String,
            chainId: String,
            signer: Signer,
            logConfig: IdosLogConfig =
                IdosLogConfig.build {
                    platformSink()
                },
        ): IdosClient =
            runCatchingDomainError {
                // Configure SDK logger
                IdosLogger.configure(logConfig)

                IdosLogger.i("Client") { "Initializing idOS SDK client for chain: $chainId" }
                IdosLogger.d("Client") { "Base URL: $baseUrl, HTTP logging: ${logConfig.httpLogLevel}" }

                IdosClient(
                    ActionExecutor(baseUrl, chainId, signer, logConfig.httpLogLevel),
                    chainId,
                )
            }
    }
}
