package org.idos

import org.idos.kwil.domain.ActionExecutor
import org.idos.kwil.security.signer.Signer

/**
 * Main entry point for the idOS SDK.
 *
 * This class provides organized access to idOS operations through object groups.
 * All operations are defined as extensions in IdosClientExtensions.kt for easy
 * discoverability in IDEs.
 *
 * Example usage:
 * ```kotlin
 * val client = IdosClient.create(
 *     baseUrl = "https://nodes.staging.idos.network",
 *     chainId = "idos-testnet",
 *     signer = EthSigner(privateKey)
 * ).getOrThrow()
 *
 * // IDE autocomplete shows available object groups
 * client.wallets.add(...)
 * client.credentials.getOwned()
 * client.accessGrants.create(...)
 * client.users.get()
 * ```
 */
class IdosClient internal constructor(
    internal val executor: ActionExecutor,
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
    )

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
         * @return Result containing IdosClient or error
         */
        fun create(
            baseUrl: String,
            chainId: String,
            signer: Signer,
        ): Result<IdosClient> =
            ActionExecutor
                .create(baseUrl, chainId, signer)
                .map { executor ->
                    IdosClient(executor, chainId)
                }
    }
}
