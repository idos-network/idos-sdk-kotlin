package org.idos.kwil.actions.generated.view

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.idos.kwil.actions.generated.NoParamsAction
import org.idos.kwil.rpc.UuidString

@Serializable
data class GetAccessGrantsOwnedResponse(
    @SerialName("id") val id: UuidString,
    @SerialName("ag_owner_user_id") val agOwnerUserId: UuidString,
    @SerialName("ag_grantee_wallet_identifier") val agGranteeWalletIdentifier: String,
    @SerialName("data_id") val dataId: UuidString,
    @SerialName("locked_until") val lockedUntil: Long,
    @SerialName("content_hash") val contentHash: String,
    @SerialName("inserter_type") val inserterType: String,
    @SerialName("inserter_id") val inserterId: String,
)

/**
 * Descriptor for the `get_access_grants_owned` action.
 *
 * This action retrieves access grants for an authenticated user.
 */
object GetAccessGrantsOwned : NoParamsAction<GetAccessGrantsOwnedResponse>() {
    override val namespace: String = "main"
    override val name: String = "get_access_grants_owned"
}
