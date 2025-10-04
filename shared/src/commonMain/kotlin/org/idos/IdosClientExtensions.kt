package org.idos

import org.idos.kwil.domain.generated.execute.AddAttribute
import org.idos.kwil.domain.generated.execute.AddAttributeParams
import org.idos.kwil.domain.generated.execute.AddCredential
import org.idos.kwil.domain.generated.execute.AddCredentialParams
import org.idos.kwil.domain.generated.execute.AddWallet
import org.idos.kwil.domain.generated.execute.AddWalletParams
import org.idos.kwil.domain.generated.execute.CreateAccessGrant
import org.idos.kwil.domain.generated.execute.CreateAccessGrantParams
import org.idos.kwil.domain.generated.execute.EditAttribute
import org.idos.kwil.domain.generated.execute.EditAttributeParams
import org.idos.kwil.domain.generated.execute.EditCredential
import org.idos.kwil.domain.generated.execute.EditCredentialParams
import org.idos.kwil.domain.generated.execute.RemoveAttribute
import org.idos.kwil.domain.generated.execute.RemoveAttributeParams
import org.idos.kwil.domain.generated.execute.RemoveCredential
import org.idos.kwil.domain.generated.execute.RemoveCredentialParams
import org.idos.kwil.domain.generated.execute.RemoveWallet
import org.idos.kwil.domain.generated.execute.RemoveWalletParams
import org.idos.kwil.domain.generated.execute.RevokeAccessGrant
import org.idos.kwil.domain.generated.execute.RevokeAccessGrantParams
import org.idos.kwil.domain.generated.execute.ShareAttribute
import org.idos.kwil.domain.generated.execute.ShareAttributeParams
import org.idos.kwil.domain.generated.execute.ShareCredential
import org.idos.kwil.domain.generated.execute.ShareCredentialParams
import org.idos.kwil.domain.generated.view.GetAccessGrantsForCredential
import org.idos.kwil.domain.generated.view.GetAccessGrantsForCredentialParams
import org.idos.kwil.domain.generated.view.GetAccessGrantsForCredentialResponse
import org.idos.kwil.domain.generated.view.GetAccessGrantsGranted
import org.idos.kwil.domain.generated.view.GetAccessGrantsGrantedParams
import org.idos.kwil.domain.generated.view.GetAccessGrantsGrantedResponse
import org.idos.kwil.domain.generated.view.GetAccessGrantsOwned
import org.idos.kwil.domain.generated.view.GetAccessGrantsOwnedResponse
import org.idos.kwil.domain.generated.view.GetAttributes
import org.idos.kwil.domain.generated.view.GetAttributesResponse
import org.idos.kwil.domain.generated.view.GetCredentialOwned
import org.idos.kwil.domain.generated.view.GetCredentialOwnedParams
import org.idos.kwil.domain.generated.view.GetCredentialOwnedResponse
import org.idos.kwil.domain.generated.view.GetCredentialShared
import org.idos.kwil.domain.generated.view.GetCredentialSharedParams
import org.idos.kwil.domain.generated.view.GetCredentialSharedResponse
import org.idos.kwil.domain.generated.view.GetCredentials
import org.idos.kwil.domain.generated.view.GetCredentialsResponse
import org.idos.kwil.domain.generated.view.GetUser
import org.idos.kwil.domain.generated.view.GetUserResponse
import org.idos.kwil.domain.generated.view.GetWallets
import org.idos.kwil.domain.generated.view.GetWalletsResponse
import org.idos.kwil.domain.generated.view.HasProfile
import org.idos.kwil.domain.generated.view.HasProfileParams
import org.idos.kwil.types.HexString
import org.idos.kwil.types.UuidString

/**
 * Extension functions for IdosClient operation groups.
 *
 * This file contains all public API operations as extensions for easy copy-paste
 * modification and code generation. Each operation group is separated for clarity.
 *
 * Pattern:
 * - View actions (queries) return Result<List<T>> or Result<T?>
 * - Execute actions (transactions) return Result<HexString>
 * - All operations are suspend functions
 */

// ============================================================================
// WALLETS
// ============================================================================

/**
 * Adds a new wallet to the user's profile.
 *
 * @param input Wallet details including address, public key, and signature
 * @return Result containing transaction hash or error
 */
suspend fun IdosClient.Wallets.add(input: AddWalletParams): Result<HexString> = executor.execute(AddWallet, input)

/**
 * Gets all wallets for the current user.
 *
 * @return Result containing list of wallets or error
 */
suspend fun IdosClient.Wallets.getAll(): Result<List<GetWalletsResponse>> =
    executor.call(
        GetWallets,
    )

/**
 * Removes a wallet from the user's profile.
 *
 * @param id Wallet ID to remove
 * @return Result containing transaction hash or error
 */
suspend fun IdosClient.Wallets.remove(id: UuidString): Result<HexString> =
    executor.execute(
        RemoveWallet,
        RemoveWalletParams(id),
    )

// ============================================================================
// CREDENTIALS
// ============================================================================

/**
 * Adds a new credential.
 *
 * @param input Credential details
 * @return Result containing transaction hash or error
 */
suspend fun IdosClient.Credentials.add(input: AddCredentialParams): Result<HexString> = executor.execute(AddCredential, input)

/**
 * Gets all credentials owned by the current user.
 *
 * @return Result containing list of owned credentials or error
 */
suspend fun IdosClient.Credentials.getAll(): Result<List<GetCredentialsResponse>> = executor.call(GetCredentials)

/**
 * Gets credential owned by the current user by id.
 *
 * @return Result containing credential or error (including not found)
 */
suspend fun IdosClient.Credentials.getOwned(id: UuidString): Result<GetCredentialOwnedResponse> =
    executor.callSingle(
        GetCredentialOwned,
        GetCredentialOwnedParams(id),
    )

/**
 * Gets all credentials shared with the current user.
 *
 * @return Result containing list of shared credentials or error
 */
suspend fun IdosClient.Credentials.getShared(id: UuidString): Result<List<GetCredentialSharedResponse>> =
    executor.call(
        GetCredentialShared,
        GetCredentialSharedParams(id),
    )

/**
 * Edits an existing credential.
 *
 * @param input Updated credential details
 * @return Result containing transaction hash or error
 */
suspend fun IdosClient.Credentials.edit(input: EditCredentialParams): Result<HexString> = executor.execute(EditCredential, input)

/**
 * Removes a credential.
 *
 * @param id Credential ID to remove
 * @return Result containing transaction hash or error
 */
suspend fun IdosClient.Credentials.remove(id: UuidString): Result<HexString> =
    executor.execute(
        RemoveCredential,
        RemoveCredentialParams(id),
    )

/**
 * Shares a credential with a grantee.
 *
 * @param input Share details including credential ID and grantee
 * @return Result containing transaction hash or error
 */
suspend fun IdosClient.Credentials.share(input: ShareCredentialParams): Result<HexString> = executor.execute(ShareCredential, input)

// ============================================================================
// ACCESS GRANTS
// ============================================================================

/**
 * Creates a new access grant.
 *
 * @param input Access grant details
 * @return Result containing transaction hash or error
 */
suspend fun IdosClient.AccessGrants.create(input: CreateAccessGrantParams): Result<HexString> = executor.execute(CreateAccessGrant, input)

/**
 * Gets all access grants owned by the current user.
 *
 * @return Result containing list of owned access grants or error
 */
suspend fun IdosClient.AccessGrants.getOwned(): Result<List<GetAccessGrantsOwnedResponse>> =
    executor.call(
        GetAccessGrantsOwned,
    )

/**
 * Gets all access grants granted by the current user.
 *
 * @return Result containing list of granted access grants or error
 */
suspend fun IdosClient.AccessGrants.getGranted(
    userId: UuidString,
    page: Int,
    size: Int,
): Result<List<GetAccessGrantsGrantedResponse>> =
    executor.call(
        GetAccessGrantsGranted,
        GetAccessGrantsGrantedParams(userId, page, size),
    )

/**
 * Gets access grants for a specific credential.
 *
 * @param credentialId Credential ID to query
 * @return Result containing list of access grants for the credential or error
 */
suspend fun IdosClient.AccessGrants.getForCredential(credentialId: UuidString): Result<List<GetAccessGrantsForCredentialResponse>> =
    executor.call(
        GetAccessGrantsForCredential,
        GetAccessGrantsForCredentialParams(credentialId),
    )

/**
 * Revokes an access grant.
 *
 * @param id Access Grant id to revoke
 * @return Result containing transaction hash or error
 */
suspend fun IdosClient.AccessGrants.revoke(id: UuidString): Result<HexString> =
    executor.execute(RevokeAccessGrant, RevokeAccessGrantParams(id))

// ============================================================================
// USERS
// ============================================================================

/**
 * Gets the current user's profile.
 *
 * @return Result containing user profile or error (including not found)
 */
suspend fun IdosClient.Users.get(): Result<GetUserResponse> = executor.callSingle(GetUser)

/**
 * Checks if the current user has a profile.
 *
 * @return Result containing true if profile exists, false otherwise, or error
 */
suspend fun IdosClient.Users.hasProfile(address: HexString): Result<Boolean> =
    executor.call(HasProfile, HasProfileParams(address.prefixedValue)).map { results ->
        results.firstOrNull()?.hasProfile ?: false
    }

// ============================================================================
// ATTRIBUTES
// ============================================================================

/**
 * Adds a new attribute.
 *
 * @param input Attribute details
 * @return Result containing transaction hash or error
 */
suspend fun IdosClient.Attributes.add(input: AddAttributeParams): Result<HexString> = executor.execute(AddAttribute, input)

/**
 * Gets all attributes for the current user.
 *
 * @return Result containing list of attributes or error
 */
suspend fun IdosClient.Attributes.getAll(): Result<List<GetAttributesResponse>> =
    executor.call(
        GetAttributes,
    )

/**
 * Edits an existing attribute.
 *
 * @param input Updated attribute details
 * @return Result containing transaction hash or error
 */
suspend fun IdosClient.Attributes.edit(input: EditAttributeParams): Result<HexString> = executor.execute(EditAttribute, input)

/**
 * Removes an attribute.
 *
 * @param id Attribute ID to remove
 * @return Result containing transaction hash or error
 */
suspend fun IdosClient.Attributes.remove(id: UuidString): Result<HexString> =
    executor.execute(
        RemoveAttribute,
        RemoveAttributeParams(id),
    )

/**
 * Shares an attribute with a grantee.
 *
 * @param input Share details including attribute ID and grantee
 * @return Result containing transaction hash or error
 */
suspend fun IdosClient.Attributes.share(input: ShareAttributeParams): Result<HexString> = executor.execute(ShareAttribute, input)

/**
 * Internal accessor for ActionExecutor.
 * Used by extension functions to access the executor from nested classes.
 */
private val IdosClient.Wallets.executor: org.idos.kwil.domain.ActionExecutor
    get() = client.executor

private val IdosClient.Credentials.executor: org.idos.kwil.domain.ActionExecutor
    get() = client.executor

private val IdosClient.AccessGrants.executor: org.idos.kwil.domain.ActionExecutor
    get() = client.executor

private val IdosClient.Users.executor: org.idos.kwil.domain.ActionExecutor
    get() = client.executor

private val IdosClient.Attributes.executor: org.idos.kwil.domain.ActionExecutor
    get() = client.executor
