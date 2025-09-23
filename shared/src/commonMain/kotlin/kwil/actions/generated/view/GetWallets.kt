package org.idos.kwil.actions.generated.view

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.idos.kwil.actions.generated.NoParamsAction
import org.idos.kwil.rpc.UuidString

// -- for EVM type, address is EVM case insensitive 20 bytes address like 0x5ccbe82FEDE13aecdA449eCA4D4dE05E45861684, and public key can be any or null
// -- for NEAR type, address is like alex.testnet, and public key is base58 in format ed25519:6YMr5ggaCe9AtiQNeh2spn8iff72QVHyaXvu4aKsyWuB
// -- for XRPL type, address is case sensitive like rHb9CJAWyB4rj91VRWn96Dk6kG4b4dtyTh, and public key is 32-byte Ed25519 public key in hex format, prefixed by `ED`

@Serializable
data class GetWalletResponse(
    @SerialName("id") val id: UuidString,
    @SerialName("user_id") val userId: UuidString,
    @SerialName("address") val address: String,
    @SerialName("public_key") val publicKey: String,
    @SerialName("wallet_type") val walletType: String,
    @SerialName("message") val message: String,
    @SerialName("signature") val signature: String,
    @SerialName("inserter") val inserter: String,
)

/**
 * Descriptor for the `get_wallets` action.
 *
 * This action retrieves all wallets for an authenticated user.
 */
object GetWallets : NoParamsAction<GetWalletResponse>() {
    override val namespace: String = "main"
    override val name: String = "get_wallets"
}
