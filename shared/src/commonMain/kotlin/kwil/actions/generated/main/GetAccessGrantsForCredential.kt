package org.idos.kwil.actions.generated.main

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.idos.kwil.actions.generated.GeneratedAction
import org.idos.kwil.transaction.PositionalTypes
import org.idos.kwil.utils.DataType
import org.idos.kwil.utils.SchemaField

/**
 * Input parameters for the [GetAccessGrantsForCredential] action.
 */
@Serializable
data class GetAccessGrantsForCredentialInput(
    /** The ID of the credential to fetch access grants for */
    @SerialName("credential_id")
    val credentialId: String,
)

/**
 * Represents a single row in the result set of [GetAccessGrantsForCredential].
 */
@Serializable
data class GetAccessGrantsForCredentialRow(
    /** Unique identifier of the access grant */
    val id: String,
    /** Owner user ID of the access grant */
    @SerialName("ag_owner_user_id")
    val agOwnerUserId: String,
    /** Wallet identifier of the grantee */
    @SerialName("ag_grantee_wallet_identifier")
    val agGranteeWalletIdentifier: String,
    /** ID of the data being accessed */
    @SerialName("data_id")
    val dataId: String,
    /** Timestamp until which the grant is locked */
    @SerialName("locked_until")
    val lockedUntil: Int,
    /** Hash of the content being accessed */
    @SerialName("content_hash")
    val contentHash: String,
    /** Type of the inserter */
    @SerialName("inserter_type")
    val inserterType: String,
    /** ID of the inserter */
    @SerialName("inserter_id")
    val inserterId: String,
)

/**
 * Descriptor for the `get_access_grants_for_credential` action.
 *
 * This action retrieves all access grants associated with a specific credential.
 */
object GetAccessGrantsForCredential :
    GeneratedAction<GetAccessGrantsForCredentialInput, GetAccessGrantsForCredentialRow> {
    override val namespace: String = "main"
    override val name: String = "get_access_grants_for_credential"

    /** The action expects a single UUID parameter (credential_id) */
    override val positionalTypes: List<SchemaField> =
        listOf(
            SchemaField("credential_id", DataType.Uuid),
        )

    override fun toNamedParams(input: GetAccessGrantsForCredentialInput): Map<String, Any?> = mapOf("credential_id" to input.credentialId)
}
