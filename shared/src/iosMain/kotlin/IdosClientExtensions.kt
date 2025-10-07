// package org.idos
//
// import org.idos.kwil.domain.ActionExecutor
// import org.idos.kwil.domain.generated.execute.AddAttributeParams
// import org.idos.kwil.domain.generated.execute.AddCredentialParams
// import org.idos.kwil.domain.generated.execute.AddWalletParams
// import org.idos.kwil.domain.generated.execute.CreateAccessGrantParams
// import org.idos.kwil.domain.generated.execute.EditAttributeParams
// import org.idos.kwil.domain.generated.execute.EditCredentialParams
// import org.idos.kwil.domain.generated.execute.ShareAttributeParams
// import org.idos.kwil.domain.generated.execute.ShareCredentialParams
// import org.idos.kwil.domain.generated.view.GetAccessGrantsForCredentialResponse
// import org.idos.kwil.domain.generated.view.GetAccessGrantsGrantedResponse
// import org.idos.kwil.domain.generated.view.GetAccessGrantsOwnedResponse
// import org.idos.kwil.domain.generated.view.GetAttributesResponse
// import org.idos.kwil.domain.generated.view.GetCredentialOwnedResponse
// import org.idos.kwil.domain.generated.view.GetCredentialSharedResponse
// import org.idos.kwil.domain.generated.view.GetCredentialsResponse
// import org.idos.kwil.domain.generated.view.GetUserResponse
// import org.idos.kwil.domain.generated.view.GetWalletsResponse
//
// /**
// * Extension functions for IdosClient operation groups.
// *
// * This file contains all public API operations as extensions for easy copy-paste
// * modification and code generation. Each operation group is separated for clarity.
// *
// * Pattern:
// * - View actions (queries) return Result<List<T>> or Result<T?>
// * - Execute actions (transactions) return Result<HexString>
// * - All operations are suspend functions
// */
//
// // ============================================================================
// // WALLETS
// // ============================================================================
//
// /**
// * Adds a new wallet to the user's profile.
// *
// * @param input Wallet details including address, public key, and signature
// * @return Result containing transaction hash or error
// */
//
// /**
// * Adds a new wallet to the user's profile.
// *
// * @param id Unique identifier for the wallet
// * @param address Wallet address
// * @param publicKey Public key of the wallet
// * @param signature Signature for verification
// * @return Result containing transaction hash or error
// */
// suspend fun IdosClientIos.Wallets.add(
//    id: UuidString,
//    address: String,
//    publicKey: String,
//    message: String,
//    signature: String,
// ): ResultInterop<HexString> = id.flatMap { client.wallets.add(AddWalletParams(it, address, publicKey, message, signature)) }.interop()
//
// /**
// * Gets all wallets for the current user.
// *
// * @return Result containing list of wallets or error
// */
// suspend fun IdosClientIos.Wallets.getAll(): ResultInterop<List<GetWalletsResponse>> = client.wallets.getAll().interop()
//
// /**
// * Removes a wallet from the user's profile.
// *
// * @param id Wallet ID to remove
// * @return Result containing transaction hash or error
// */
// suspend fun IdosClientIos.Wallets.remove(id: UuidString): ResultInterop<HexString> = id.flatMap { client.wallets.remove(it) }.interop()
//
// // ============================================================================
// // CREDENTIALS
// // ============================================================================
//
// /**
// * Adds a new credential.
// *
// * @param input Credential details
// * @return Result containing transaction hash or error
// */
//
// /**
// * Adds a new credential.
// *
// * @param id Unique identifier for the credential
// * @param issuerAuthPublicKey Issuer's authentication public key
// * @param encryptorPublicKey Encryptor's public key
// * @param content Encrypted credential data
// * @param publicNotes Public notes about the credential
// * @param publicNotesSignature Signature for the public notes
// * @return Result containing transaction hash or error
// */
// suspend fun IdosClientIos.Credentials.add(
//    id: UuidString,
//    issuerAuthPublicKey: String,
//    encryptorPublicKey: String,
//    content: String,
//    publicNotes: String,
//    publicNotesSignature: String,
//    broaderSignature: String,
// ): ResultInterop<HexString> =
//    id
//        .flatMap {
//            client.credentials.add(
//                AddCredentialParams(
//                    id = it,
//                    issuerAuthPublicKey = issuerAuthPublicKey,
//                    encryptorPublicKey = encryptorPublicKey,
//                    content = content,
//                    publicNotes = publicNotes,
//                    publicNotesSignature = publicNotesSignature,
//                    broaderSignature = broaderSignature,
//                ),
//            )
//        }.interop()
//
// /**
// * Gets all credentials owned by the current user.
// *
// * @return Result containing list of owned credentials or error
// */
// suspend fun IdosClientIos.Credentials.getAll(): ResultInterop<List<GetCredentialsResponse>> = client.credentials.getAll().interop()
//
// /**
// * Gets credential owned by the current user by id.
// *
// * @return Result containing credential or error (including not found)
// */
// suspend fun IdosClientIos.Credentials.getOwned(id: UuidString): ResultInterop<GetCredentialOwnedResponse> =
//    id.flatMap { client.credentials.getOwned(it) }.interop()
//
// /**
// * Gets all credentials shared with the current user.
// *
// * @return Result containing list of shared credentials or error
// */
//
// /**
// * Gets all shared credentials for a specific credential ID.
// *
// * @param id Credential ID to query shared access
// * @return Result containing list of shared credentials or error
// */
// suspend fun IdosClientIos.Credentials.getShared(id: UuidString): ResultInterop<List<GetCredentialSharedResponse>> =
//    id.flatMap { client.credentials.getShared(it) }.interop()
//
// /**
// * Edits an existing credential.
// *
// * @param input Updated credential details
// * @return Result containing transaction hash or error
// */
//
// /**
// * Edits an existing credential.
// *
// * @param id Credential ID to edit
// * @param publicNotes Updated public notes
// * @param publicNotesSignature Signature for the updated public notes
// * @return Result containing transaction hash or error
// */
// suspend fun IdosClientIos.Credentials.edit(
//    id: UuidString,
//    publicNotes: String,
//    publicNotesSignature: String,
//    broaderSignature: String,
//    content: String,
//    encryptorPublicKey: String,
//    issuerAuthPublicKey: String,
// ): ResultInterop<HexString> =
//    id
//        .flatMap {
//            client.credentials.edit(
//                EditCredentialParams(
//                    it,
//                    publicNotes,
//                    publicNotesSignature,
//                    broaderSignature,
//                    content,
//                    encryptorPublicKey,
//                    issuerAuthPublicKey,
//                ),
//            )
//        }.interop()
//
// /**
// * Removes a credential.
// *
// * @param id Credential ID to remove
// * @return Result containing transaction hash or error
// */
// suspend fun IdosClientIos.Credentials.remove(id: UuidString): ResultInterop<HexString> =
//    id.flatMap { client.credentials.remove(it) }.interop()
//
// /**
// * Shares a credential with a grantee.
// *
// * @param input Share details including credential ID and grantee
// * @return Result containing transaction hash or error
// */
//
// /**
// * Shares a credential with a grantee.
// *
// * @param id Share ID
// * @param originalCredentialId Original credential ID being shared
// * @param publicNotes Public notes about the shared credential
// * @param publicNotesSignature Signature for the public notes
// * @param encryptedCredential Encrypted credential data for the grantee
// * @param granteeWalletIdentifier Identifier for the grantee's wallet
// * @param expiresAt Expiration timestamp for the share
// * @return Result containing transaction hash or error
// */
// suspend fun IdosClientIos.Credentials.share(
//    id: UuidString,
//    originalCredentialId: UuidString,
//    publicNotes: String,
//    publicNotesSignature: String,
//    broaderSignature: String,
//    content: String,
//    contentHash: String,
//    encryptorPublicKey: String,
//    issuerAuthPublicKey: String,
//    granteeWalletIdentifier: String,
//    lockedUntil: Int,
// ): ResultInterop<HexString> =
//    Pair(id, originalCredentialId)
//        .flatMap {
//            client.credentials.share(
//                ShareCredentialParams(
//                    id = it.first,
//                    originalCredentialId = it.second,
//                    publicNotes = publicNotes,
//                    publicNotesSignature = publicNotesSignature,
//                    broaderSignature = broaderSignature,
//                    content = content,
//                    contentHash = contentHash,
//                    encryptorPublicKey = encryptorPublicKey,
//                    issuerAuthPublicKey = issuerAuthPublicKey,
//                    granteeWalletIdentifier = granteeWalletIdentifier,
//                    lockedUntil = lockedUntil,
//                ),
//            )
//        }.interop()
//
// // ============================================================================
// // ACCESS GRANTS
// // ============================================================================
//
// /**
// * Creates a new access grant.
// *
// * @param input Access grant details
// * @return Result containing transaction hash or error
// */
//
// /**
// * Creates a new access grant.
// *
// * @param granteeWalletIdentifier Identifier for the grantee's wallet
// * @param dataId ID of the data being granted access to
// * @param lockedUntil Timestamp until which the grant is locked
// * @param dataType Type of data being granted access to
// * @return Result containing transaction hash or error
// */
// suspend fun IdosClientIos.AccessGrants.create(
//    granteeWalletIdentifier: String,
//    dataId: UuidString,
//    lockedUntil: Int,
//    contentHash: String,
//    inserterType: String,
//    inserterId: String,
// ): ResultInterop<HexString> =
//    dataId
//        .flatMap {
//            client.accessGrants.create(
//                CreateAccessGrantParams(
//                    granteeWalletIdentifier = granteeWalletIdentifier,
//                    dataId = it,
//                    lockedUntil = lockedUntil,
//                    contentHash = contentHash,
//                    inserterType = inserterType,
//                    inserterId = inserterId,
//                ),
//            )
//        }.interop()
//
// /**
// * Gets all access grants owned by the current user.
// *
// * @return Result containing list of owned access grants or error
// */
// suspend fun IdosClientIos.AccessGrants.getOwned(): ResultInterop<List<GetAccessGrantsOwnedResponse>> =
//    client.accessGrants.getOwned().interop()
//
// /**
// * Gets all access grants granted by the current user.
// *
// * @param userId The user ID to query granted access grants for
// * @param page The page number for pagination
// * @param size The number of items per page
// * @return Result containing list of granted access grants or error
// */
// suspend fun IdosClientIos.AccessGrants.getGranted(
//    userId: UuidString,
//    page: Int,
//    size: Int,
// ): ResultInterop<List<GetAccessGrantsGrantedResponse>> = userId.flatMap { client.accessGrants.getGranted(it, page, size) }.interop()
//
// /**
// * Gets access grants for a specific credential.
// *
// * @param credentialId Credential ID to query
// * @return Result containing list of access grants for the credential or error
// */
// suspend fun IdosClientIos.AccessGrants.getForCredential(
//    credentialId: UuidString,
// ): ResultInterop<List<GetAccessGrantsForCredentialResponse>> = credentialId.flatMap { client.accessGrants.getForCredential(it) }.interop()
//
// /**
// * Revokes an access grant.
// *
// * @param id Access Grant id to revoke
// * @return Result containing transaction hash or error
// */
// suspend fun IdosClientIos.AccessGrants.revoke(id: UuidString): ResultInterop<HexString> =
//    id.flatMap { client.accessGrants.revoke(it) }.interop()
//
// // ============================================================================
// // USERS
// // ============================================================================
//
// /**
// * Gets the current user's profile.
// *
// * @return Result containing user profile or error (including not found)
// */
// suspend fun IdosClientIos.Users.get(): ResultInterop<GetUserResponse> = client.users.get().interop()
//
// /**
// * Checks if the current user has a profile.
// *
// * @param address The address to check for a profile
// * @return Result containing true if profile exists, false otherwise, or error
// */
// suspend fun IdosClientIos.Users.hasProfile(address: HexString): ResultInterop<Boolean> =
//    address.flatMap { client.users.hasProfile(it) }.interop()
//
// // ============================================================================
// // ATTRIBUTES
// // ============================================================================
//
// /**
// * Adds a new attribute.
// *
// * @param input Attribute details
// * @return Result containing transaction hash or error
// */
//
// /**
// * Adds a new attribute.
// *
// * @param input Attribute details
// * @return Result containing transaction hash or error
// */
//
// /**
// * Adds a new attribute.
// *
// * @param id Unique identifier for the attribute
// * @param attributeKey Key/name of the attribute
// * @param value Value of the attribute
// * @return Result containing transaction hash or error
// */
// suspend fun IdosClientIos.Attributes.add(
//    id: UuidString,
//    attributeKey: String,
//    value: String,
// ): ResultInterop<HexString> =
//    id
//        .flatMap {
//            client.attributes
//                .add(
//                    AddAttributeParams(it, attributeKey, value),
//                )
//        }.interop()
//
// /**
// * Gets all attributes for the current user.
// *
// * @return Result containing list of attributes or error
// */
//
// /**
// * Gets all attributes for the current user.
// *
// * @return Result containing list of attributes or error
// */
// suspend fun IdosClientIos.Attributes.getAll(): ResultInterop<List<GetAttributesResponse>> = client.attributes.getAll().interop()
//
// /**
// * Edits an existing attribute.
// *
// * @param input Updated attribute details
// * @return Result containing transaction hash or error
// */
//
// /**
// * Edits an existing attribute.
// *
// * @param input Updated attribute details
// * @return Result containing transaction hash or error
// */
//
// /**
// * Edits an existing attribute.
// *
// * @param id Attribute ID to edit
// * @param attributeKey Updated attribute key/name
// * @param value Updated attribute value
// * @return Result containing transaction hash or error
// */
// suspend fun IdosClientIos.Attributes.edit(
//    id: UuidString,
//    attributeKey: String,
//    value: String,
// ): ResultInterop<HexString> =
//    id
//        .flatMap {
//            client.attributes.edit(
//                EditAttributeParams(it, attributeKey, value),
//            )
//        }.interop()
//
// /**
// * Removes an attribute.
// *
// * @param id Attribute ID to remove
// * @return Result containing transaction hash or error
// */
//
// /**
// * Removes an attribute.
// *
// * @param id Attribute ID to remove
// * @return Result containing transaction hash or error
// */
// suspend fun IdosClientIos.Attributes.remove(id: UuidString): ResultInterop<HexString> =
//    id.flatMap { client.attributes.remove(it) }.interop()
//
// /**
// * Shares an attribute with a grantee.
// *
// * @param input Share details including attribute ID and grantee
// * @return Result containing transaction hash or error
// */
//
// /**
// * Shares an attribute with a grantee.
// *
// * @param input Share details including attribute ID and grantee
// * @return Result containing transaction hash or error
// */
//
// /**
// * Shares an attribute with a grantee.
// *
// * @param id Share ID
// * @param originalAttributeId Original attribute ID being shared
// * @param attributeKey Key/name of the attribute
// * @param granteeWalletIdentifier Identifier for the grantee's wallet
// * @param expiresAt Expiration timestamp for the share
// * @return Result containing transaction hash or error
// */
// suspend fun IdosClientIos.Attributes.share(
//    id: UuidString,
//    originalAttributeId: UuidString,
//    attributeKey: String,
//    value: String,
// ): ResultInterop<HexString> =
//    Pair(id, originalAttributeId)
//        .flatMap {
//            client.attributes.share(
//                ShareAttributeParams(
//                    id = it.first,
//                    originalAttributeId = it.second,
//                    attributeKey = attributeKey,
//                    value = value,
//                ),
//            )
//        }.interop()
//
// /**
// * Internal accessor for ActionExecutor.
// * Used by extension functions to access the executor from nested classes.
// */
// private val IdosClientIos.Wallets.executor: ActionExecutor
//    get() = client.executor
//
// private val IdosClientIos.Credentials.executor: ActionExecutor
//    get() = client.executor
//
// private val IdosClientIos.AccessGrants.executor: ActionExecutor
//    get() = client.executor
//
// private val IdosClientIos.Users.executor: ActionExecutor
//    get() = client.executor
//
// private val IdosClientIos.Attributes.executor: ActionExecutor
//    get() = client.executor
