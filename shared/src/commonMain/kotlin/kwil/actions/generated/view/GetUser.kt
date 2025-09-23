package org.idos.kwil.actions.generated.view

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.idos.kwil.actions.generated.NoParamsAction
import org.idos.kwil.rpc.UuidString

@Serializable
data class GetUserResponse(
    @SerialName("id") val id: UuidString,
    @SerialName("recipient_encryption_public_key") val recipientEncryptionPublicKey: String,
    @SerialName("encryption_password_store") val encryptionPasswordStore: String,
)

/**
 * Descriptor for the `get_user` action.
 *
 * This action retrieves user info for an authenticated user.
 */
object GetUser : NoParamsAction<GetUserResponse>() {
    override val namespace: String = "main"
    override val name: String = "get_user"
}
