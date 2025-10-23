package org.idos.enclave.mpc

import kotlinx.serialization.Serializable
import org.idos.crypto.eip712.TypedDataField

/**
 * Base class for all MPC request types.
 * Each request can generate its corresponding EIP-712 signature message.
 */
sealed class MpcRequest {
    /**
     * Convert this request to a signable EIP-712 message.
     *
     * @param contractAddress The MPC contract address
     * @return SignatureMessage ready to be signed
     */
    abstract fun toSignableMessage(contractAddress: String): SignatureMessage<out MpcRequest>
}

/**
 * Request to download a secret from MPC nodes.
 *
 * @param recoveringAddress Formatted address requesting the download (e.g., "eip712:0x..." or "NEAR:...")
 * @param timestamp Request timestamp in milliseconds
 * @param publicKey Ephemeral public key for encrypting the response (hex string with 0x prefix)
 */
@Serializable
data class DownloadRequest(
    val recoveringAddress: String,
    val timestamp: Long,
    val publicKey: String,
) : MpcRequest() {
    override fun toSignableMessage(contractAddress: String) =
        SignatureMessage(
            domain = getTypedDataDomain(contractAddress),
            types =
                mapOf(
                    "DownloadSignatureMessage" to
                        listOf(
                            TypedDataField("recovering_address", "string"),
                            TypedDataField("timestamp", "uint64"),
                            TypedDataField("public_key", "bytes32"),
                        ),
                ),
            primaryType = "DownloadSignatureMessage",
            messageValue = this,
            serializer = serializer(),
        )
}

/**
 * Request to upload a secret to MPC nodes.
 *
 * @param shareCommitments Keccak256 commitments of blinded shares (hex strings with 0x prefix)
 * @param recoveringAddresses List of formatted addresses that can recover this secret
 */
@Serializable
data class UploadRequest(
    val shareCommitments: List<String>,
    val recoveringAddresses: List<String>,
) : MpcRequest() {
    override fun toSignableMessage(contractAddress: String) =
        SignatureMessage(
            domain = getTypedDataDomain(contractAddress),
            types =
                mapOf(
                    "UploadSignatureMessage" to
                        listOf(
                            TypedDataField("share_commitments", "bytes32[]"),
                            TypedDataField("recovering_addresses", "string[]"),
                        ),
                ),
            primaryType = "UploadSignatureMessage",
            messageValue = this,
            serializer = serializer(),
        )
}

/**
 * Request to add a recovery address to an existing secret.
 *
 * @param recoveringAddress Current formatted address making the request
 * @param addressToAdd Formatted address to add (e.g., "eip712:0x..." or "NEAR:...")
 * @param timestamp Request timestamp in milliseconds
 */
@Serializable
data class AddAddressRequest(
    val recoveringAddress: String,
    val addressToAdd: String,
    val timestamp: Long,
) : MpcRequest() {
    override fun toSignableMessage(contractAddress: String) =
        SignatureMessage(
            domain = getTypedDataDomain(contractAddress),
            types =
                mapOf(
                    "AddAddressSignatureMessage" to
                        listOf(
                            TypedDataField("recovering_address", "string"),
                            TypedDataField("address_to_add", "string"),
                            TypedDataField("timestamp", "uint64"),
                        ),
                ),
            primaryType = "AddAddressSignatureMessage",
            messageValue = this,
            serializer = serializer(),
        )
}

/**
 * Request to remove a recovery address from an existing secret.
 *
 * @param recoveringAddress Current formatted address making the request
 * @param addressToRemove Formatted address to remove
 * @param timestamp Request timestamp in milliseconds
 */
@Serializable
data class RemoveAddressRequest(
    val recoveringAddress: String,
    val addressToRemove: String,
    val timestamp: Long,
) : MpcRequest() {
    override fun toSignableMessage(contractAddress: String) =
        SignatureMessage(
            domain = getTypedDataDomain(contractAddress),
            types =
                mapOf(
                    "RemoveAddressSignatureMessage" to
                        listOf(
                            TypedDataField("recovering_address", "string"),
                            TypedDataField("address_to_remove", "string"),
                            TypedDataField("timestamp", "uint64"),
                        ),
                ),
            primaryType = "RemoveAddressSignatureMessage",
            messageValue = this,
            serializer = serializer(),
        )
}

/**
 * Request to update the list of recovery addresses for a secret.
 *
 * @param recoveringAddresses New list of formatted recovery addresses
 * @param timestamp Request timestamp in milliseconds
 */
@Serializable
data class UpdateWalletsRequest(
    val recoveringAddresses: List<String>,
    val timestamp: Long,
) : MpcRequest() {
    override fun toSignableMessage(contractAddress: String) =
        SignatureMessage(
            domain = getTypedDataDomain(contractAddress),
            types =
                mapOf(
                    "UpdateWalletsSignatureMessage" to
                        listOf(
                            TypedDataField("recovering_addresses", "string[]"),
                            TypedDataField("timestamp", "uint64"),
                        ),
                ),
            primaryType = "UpdateWalletsSignatureMessage",
            messageValue = this,
            serializer = serializer(),
        )
}
