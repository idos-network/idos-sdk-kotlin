package org.idos.kwil.actions.generated.view

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.idos.kwil.actions.ViewAction
import org.idos.kwil.transaction.PositionalParams
import org.idos.kwil.transaction.PositionalTypes
import org.idos.kwil.utils.DataType

@Serializable
data class HasProfileResponse(
    @SerialName("has_profile")
    val hasProfile: Boolean,
)

/**
 * Input parameters for the [HasProfile] action.
 */
@Serializable
data class HasProfileInput(
    /** The address of the user to fetch for */
    @SerialName("address")
    val address: String,
)

/**
 * Descriptor for the `has_profile` action.
 *
 * This action checks whether a user with the given address has a profile set up in the application's database.
 */
object HasProfile :
    ViewAction<HasProfileInput, HasProfileResponse> {
    override val namespace: String = "main"
    override val name: String = "has_profile"

    /** The action expects a single text parameter (address) */
    override val positionalTypes: PositionalTypes = listOf(DataType.Text)

    override fun toPositionalParams(input: HasProfileInput): PositionalParams = listOf(input.address)
}
