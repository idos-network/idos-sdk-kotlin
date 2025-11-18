package org.idos.crypto.eip712

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * EIP-712 TypedDataDomain for contract interactions.
 *
 * @param name The domain name
 * @param version The domain version
 * @param verifyingContract The contract address (with 0x prefix)
 */
@Serializable
data class TypedDataDomain(
    val name: String,
    val version: String,
    val verifyingContract: String,
    val chainId: Int? = null,
)

/**
 * EIP-712 typed data field definition.
 *
 * @param name The field name
 * @param type The Solidity type (e.g., "string", "uint64", "bytes32[]")
 */
@Serializable
data class TypedDataField(
    val name: String,
    val type: String,
)

/**
 * Full EIP-712 typed data structure for EVM signing.
 *
 * @param domain The EIP-712 domain separator parameters
 * @param types Type definitions mapping type names to their field arrays
 * @param primaryType The primary type name to hash
 * @param message The message data as a JSON object
 */
@Serializable
data class TypedData(
    val domain: TypedDataDomain,
    val types: Map<String, List<TypedDataField>>,
    val primaryType: String,
    val message: JsonObject,
) {
    /**
     * Serialize this TypedData to a JSON string.
     * Useful for passing to external wallets for signing.
     */
    fun toJsonString(): String {
        return Json.encodeToString(this)
    }
}
