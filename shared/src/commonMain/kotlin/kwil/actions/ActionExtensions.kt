package org.idos.kwil.actions

import org.idos.kwil.KwilActionClient
import org.idos.kwil.actions.generated.execute.RevokeAccessGrant
import org.idos.kwil.actions.generated.execute.RevokeAccessGrantInput
import org.idos.kwil.actions.generated.main.GetCredentialOwned
import org.idos.kwil.actions.generated.main.GetCredentialOwnedInput
import org.idos.kwil.actions.generated.main.GetCredentialOwnedResponse
import org.idos.kwil.actions.generated.view.GetAccessGrantsOwned
import org.idos.kwil.actions.generated.view.GetAccessGrantsOwnedResponse
import org.idos.kwil.actions.generated.view.GetCredentials
import org.idos.kwil.actions.generated.view.GetCredentialsResponse
import org.idos.kwil.actions.generated.view.GetUser
import org.idos.kwil.actions.generated.view.GetUserResponse
import org.idos.kwil.actions.generated.view.GetWalletResponse
import org.idos.kwil.actions.generated.view.GetWallets
import org.idos.kwil.actions.generated.view.HasProfile
import org.idos.kwil.actions.generated.view.HasProfileInput
import org.idos.kwil.actions.generated.view.HasProfileResponse
import org.idos.kwil.rpc.HexString
import org.idos.kwil.rpc.UuidString

// View actions
suspend fun KwilActionClient.hasUserProfile(address: String): HasProfileResponse =
    callAction(
        action = HasProfile,
        input = HasProfileInput(address),
    ).single()

suspend fun KwilActionClient.getWallets(): List<GetWalletResponse> = callAction(GetWallets)

suspend fun KwilActionClient.getCredentials(): List<GetCredentialsResponse> = callAction(GetCredentials)

suspend fun KwilActionClient.getUser(): GetUserResponse = callAction(GetUser).single()

suspend fun KwilActionClient.getAccessGrantsOwned(): List<GetAccessGrantsOwnedResponse> = callAction(GetAccessGrantsOwned)

suspend fun KwilActionClient.getCredentialOwned(id: UuidString): GetCredentialOwnedResponse =
    callAction(GetCredentialOwned, GetCredentialOwnedInput(id)).single()

// Execute actions
suspend fun KwilActionClient.revokeAccessGrant(accessId: UuidString): HexString =
    executeAction(RevokeAccessGrant, RevokeAccessGrantInput(accessId))
