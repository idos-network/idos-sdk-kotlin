package org.idos.kwil.actions

import org.idos.kwil.KwilActionClient
import org.idos.kwil.actions.generated.GeneratedAction
import org.idos.kwil.actions.generated.main.GetAccessGrantsForCredential
import org.idos.kwil.actions.generated.main.GetAccessGrantsForCredentialInput
import org.idos.kwil.actions.generated.main.GetAccessGrantsForCredentialRow

/**
 * Fetches access grants for a specific credential with Result wrapper.
 *
 * @param credentialId The ID of the credential to fetch grants for
 * @return Result containing either the list of access grants or an exception
 */
suspend fun KwilActionClient.getAccessGrantsForCredential(credentialId: String): Result<List<GetAccessGrantsForCredentialRow>> =
    this.callActionWithResult(
        action = GetAccessGrantsForCredential,
        input = GetAccessGrantsForCredentialInput(credentialId),
    )
