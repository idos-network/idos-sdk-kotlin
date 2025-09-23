package org.idos.kwil.actions

import org.idos.kwil.KwilActionClient
import org.idos.kwil.actions.generated.execute.RevokeAccessGrant
import org.idos.kwil.actions.generated.execute.RevokeAccessGrantParams
import org.idos.kwil.actions.generated.view.GetAccessGrantsOwned
import org.idos.kwil.actions.generated.view.GetAccessGrantsOwnedResponse
import org.idos.kwil.actions.generated.view.GetCredentialOwned
import org.idos.kwil.actions.generated.view.GetCredentialOwnedParams
import org.idos.kwil.actions.generated.view.GetCredentialOwnedResponse
import org.idos.kwil.actions.generated.view.GetCredentials
import org.idos.kwil.actions.generated.view.GetCredentialsResponse
import org.idos.kwil.actions.generated.view.GetUser
import org.idos.kwil.actions.generated.view.GetUserResponse
import org.idos.kwil.actions.generated.view.GetWallets
import org.idos.kwil.actions.generated.view.GetWalletsResponse
import org.idos.kwil.actions.generated.view.HasProfile
import org.idos.kwil.actions.generated.view.HasProfileParams
import org.idos.kwil.actions.generated.view.HasProfileResponse
import org.idos.kwil.rpc.HexString
import org.idos.kwil.rpc.UuidString

// View actions
suspend fun KwilActionClient.hasUserProfile(address: String): HasProfileResponse =
    callAction(
        action = HasProfile,
        input = HasProfileParams(address),
    ).single()

suspend fun KwilActionClient.getWallets(): List<GetWalletsResponse> = callAction(GetWallets)

suspend fun KwilActionClient.getCredentials(): List<GetCredentialsResponse> = callAction(GetCredentials)

suspend fun KwilActionClient.getUser(): GetUserResponse = callAction(GetUser).single()

suspend fun KwilActionClient.getAccessGrantsOwned(): List<GetAccessGrantsOwnedResponse> = callAction(GetAccessGrantsOwned)

suspend fun KwilActionClient.getCredentialOwned(id: UuidString): GetCredentialOwnedResponse =
    callAction(GetCredentialOwned, GetCredentialOwnedParams(id)).single()

// Execute actions
suspend fun KwilActionClient.revokeAccessGrant(accessId: UuidString): HexString =
    executeAction(RevokeAccessGrant, RevokeAccessGrantParams(accessId))
