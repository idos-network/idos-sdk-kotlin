package org.idos.kwil.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.idos.kwil.rpc.Base64String
import org.idos.kwil.signer.SignatureType

// https://github.com/trufnetwork/kwil-js/blob/main/src/core/signature.ts#L5
@Serializable
data class Signature(
    val sig: Base64String?,
    val type: SignatureType,
)

@Serializable
enum class SerializationType(
    value: String,
) {
    @SerialName("invalid")
    INVALID_SERIALIZATION_TYPE("invalid"),

    @SerialName("concat")
    SIGNED_MSG_CONCAT("concat"),

    @SerialName("eip712")
    SIGNED_MSG_EIP712("eip712"),
}
