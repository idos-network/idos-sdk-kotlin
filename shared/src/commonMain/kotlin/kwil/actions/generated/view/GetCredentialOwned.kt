package org.idos.kwil.actions.generated.main

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.idos.kwil.actions.ViewAction
import org.idos.kwil.rpc.UuidString
import org.idos.kwil.transaction.PositionalParams
import org.idos.kwil.transaction.PositionalTypes
import org.idos.kwil.utils.DataType

/**
 * Input parameters for the [GetCredentialOwnedInput] action.
 */
data class GetCredentialOwnedInput(
    /** The ID of the credential to fetch by id */
    val credentialId: UuidString,
)

@Serializable
data class GetCredentialOwnedResponse(
    @SerialName("id") val id: UuidString,
    @SerialName("user_id") val userId: UuidString,
    @SerialName("public_notes") val publicNotes: String,
    @SerialName("content") val content: String,
    @SerialName("encryptor_public_key") val encryptorPublicKey: String,
    @SerialName("issuer_auth_public_key") val issuerAuthPublicKey: String,
    @SerialName("inserter") val inserter: String? = null,
)

/**
 * Descriptor for the `get_credential_owned` action.
 *
 * This action retrieves a single credential by its id.
 */
object GetCredentialOwned : ViewAction<GetCredentialOwnedInput, GetCredentialOwnedResponse> {
    override val namespace: String = "main"
    override val name: String = "get_credential_owned"

    /** The action expects a single UUID parameter (id) */
    override val positionalTypes: PositionalTypes = listOf(DataType.Uuid)

    override fun toPositionalParams(input: GetCredentialOwnedInput): PositionalParams = listOf(input.credentialId.value)
}
