package org.idos.kwil.actions.generated.execute

import org.idos.kwil.actions.generated.ExecuteAction
import org.idos.kwil.rpc.UuidString
import org.idos.kwil.transaction.PositionalParams
import org.idos.kwil.transaction.PositionalTypes
import org.idos.kwil.utils.DataType

/**
 * Input parameters for the [RevokeAccessGrant] action.
 */
data class RevokeAccessGrantInput(
    /** The ID of the access grant to remove */
    val id: UuidString,
)

/**
 * Descriptor for the `revoke_access_grant` action.
 *
 * This action revokes a single access grant by its id.
 */
object RevokeAccessGrant : ExecuteAction<RevokeAccessGrantInput> {
    override val namespace: String = "main"
    override val name: String = "revoke_access_grant"
    override val description: String = "Revoke an Access Grant from idOS"

    /** The action expects a single UUID parameter (id) */
    override val positionalTypes: PositionalTypes = listOf(DataType.Uuid)

    override fun toPositionalParams(input: RevokeAccessGrantInput): PositionalParams = listOf(input.id.value)
}
