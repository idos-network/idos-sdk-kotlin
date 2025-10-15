package org.idos.enclave.mpc

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.idos.crypto.eip712.TypedData
import org.idos.crypto.eip712.TypedDataDomain
import org.idos.crypto.eip712.TypedDataField

/**
 * Complete signature message with EIP-712 structured data.
 *
 * Uses generics to avoid manual type matching when serializing requests.
 *
 * @param T The specific MpcRequest type (DownloadRequest, UploadRequest, etc.)
 * @param domain The EIP-712 domain
 * @param types The type definitions (e.g., "DownloadSignatureMessage" -> [fields])
 * @param primaryType The primary type name
 * @param messageValue The request object
 * @param serializer The KSerializer for the request type
 */
data class SignatureMessage<T : MpcRequest>(
    val domain: TypedDataDomain,
    val types: Map<String, List<TypedDataField>>,
    val primaryType: String,
    val messageValue: T,
    val serializer: KSerializer<T>,
) {
    @OptIn(ExperimentalSerializationApi::class)
    private val json =
        Json {
            prettyPrint = false
            namingStrategy = JsonNamingStrategy.SnakeCase
        }

    /**
     * Convert to TypedData for EIP-712 signing.
     * Used for EVM signers.
     *
     * @return TypedData structure ready for signing
     */
    fun toTypedData(): TypedData {
        val messageJson = serializeMessageValue()

        return TypedData(
            domain = domain,
            types = types,
            primaryType = primaryType,
            message = messageJson,
        )
    }

    /**
     * Serialize just the message value.
     * Used for NEAR and XRPL signers.
     *
     * @return JSON string of just the message value
     */
    fun toValueJsonString(): String = json.encodeToString(serializer, messageValue)

    /**
     * Convert request object to JsonObject for EIP-712 structure.
     */
    private fun serializeMessageValue(): JsonObject = json.encodeToJsonElement(serializer, messageValue).jsonObject
}

/**
 * Get the shared TypedDataDomain for all MPC operations.
 * Important!! The partisia chain uses 21byte address, while ETH is 20bytes, so we take the last 20bytes
 * otherwise the signature verification would fail on BE
 *
 * @param contractAddress The MPC contract address
 * @return TypedDataDomain instance
 */
fun getTypedDataDomain(contractAddress: String): TypedDataDomain =
    TypedDataDomain(
        name = "idOS secret store contract",
        version = "1",
        verifyingContract = "0x${contractAddress.takeLast(40)}",
    )
