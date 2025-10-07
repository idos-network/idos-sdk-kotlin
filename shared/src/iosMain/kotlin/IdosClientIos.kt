package org.idos

import org.idos.kwil.security.signer.Signer

class IdosClientIos(
    internal val client: IdosClient,
) {
    companion object {
        fun create(
            baseUrl: String,
            chainId: String,
            signer: Signer,
        ): ResultInterop<IdosClientIos> = runCatching { IdosClientIos(IdosClient.create(baseUrl, chainId, signer)) }.interop()
    }

    /**
     * Wallet operations group.
     *
     * Extensions in IdosClientExtensions.kt:
     * - add(input): Add a new wallet
     * - getAll(): Get all wallets for current user
     * - remove(id): Remove a wallet
     */
    val wallets = Wallets(this.client)

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
    val credentials = Credentials(this.client)

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
    val accessGrants = AccessGrants(this.client)

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
    val users = Users(this.client)

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
    val attributes = Attributes(this.client)

    class Attributes internal constructor(
        internal val client: IdosClient,
    )
}
