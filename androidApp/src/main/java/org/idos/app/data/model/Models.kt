package org.idos.app.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.idos.enclave.EnclaveKeyType
import org.idos.kwil.types.HexString
import org.idos.kwil.types.UuidString
import org.kethereum.model.Address

data class Credential(
    val id: UuidString,
    val notes: Notes,
)

data class Notes(
    val id: UuidString,
    val type: String,
    val level: String,
    val status: String,
    val issuer: String,
)

data class CredentialDetail(
    val id: UuidString,
    val content: String,
    val encryptorPublicKey: String,
)

data class Wallet(
    val address: String,
    val network: String,
)

// use plain string for address
@Serializable
data class UserModel(
    val id: UuidString,
    @Serializable(with = AddressSerializer::class) val walletAddress: Address,
    val enclaveKeyType: EnclaveKeyType?,
    val lastUpdated: Long = System.currentTimeMillis(),
)

object AddressSerializer : KSerializer<Address> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Address", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Address,
    ) {
        encoder.encodeString(value.hex)
    }

    override fun deserialize(decoder: Decoder): Address = Address(decoder.decodeString())
}
