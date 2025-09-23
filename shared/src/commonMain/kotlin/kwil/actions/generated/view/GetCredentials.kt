package org.idos.kwil.actions.generated.view

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.idos.kwil.actions.NoParamsAction
import org.idos.kwil.rpc.UuidString

@Serializable
data class GetCredentialsResponse(
    @SerialName("id") val id: UuidString,
    @SerialName("user_id") val userId: UuidString,
    @SerialName("public_notes") val publicNotes: String,
    @SerialName("issuer_auth_public_key") val issuerAuthPublicKey: String,
    @SerialName("inserter") val inserter: String? = null,
    @SerialName("original_id") val originalId: UuidString? = null,
)

/**
 * Descriptor for the `get_credentials` action.
 *
 * This action retrieves all credentials for an authenticated user.
 */
object GetCredentials : NoParamsAction<GetCredentialsResponse>() {
    override val namespace: String = "main"
    override val name: String = "get_credentials"
}
