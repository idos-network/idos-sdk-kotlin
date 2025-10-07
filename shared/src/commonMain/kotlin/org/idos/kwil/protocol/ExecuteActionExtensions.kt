package org.idos.kwil.protocol

import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.json.Json
import org.idos.kwil.domain.generated.ExecuteAction
import org.idos.kwil.security.signer.Signer
import org.idos.kwil.serialization.encodeExecuteAction
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.HexString
import org.kotlincrypto.hash.sha2.SHA256
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Extension functions for KwilProtocol to execute mutative actions (transactions).
 *
 * Execute actions modify database state and are mined on the blockchain.
 */

/**
 * Executes a mutative action (transaction).
 *
 * @param action The execute action descriptor
 * @param input Action input parameters
 * @param signer Signer for transaction authentication
 * @param synchronous If true, waits for transaction commit
 * @return Transaction hash
 */
suspend fun <I> KwilProtocol.executeAction(
    action: ExecuteAction<I>,
    input: I,
    signer: Signer,
    synchronous: Boolean = true,
): HexString {
    // 1. Get account to fetch nonce
    val account = getAccount(signer.accountId())
    val nonce = account.nonce + 1

    // 2. Encode the action payload
    val payload =
        encodeExecuteAction(
            action.namespace,
            action.name,
            action.toPositionalParams(input),
            action.positionalTypes,
        )

    // 3. Build transaction
    val tx =
        buildTransaction(
            payload = payload,
            description = action.name,
            nonce = nonce,
            signer = signer,
        )

    // 4. Sign the transaction
    val signedTx = signTransaction(tx, signer)

    // 5. Broadcast
    val syncType = if (synchronous) BroadcastSyncType.COMMIT else BroadcastSyncType.SYNC
    val response = broadcast(signedTx, syncType)

    // 6. Check transaction result if synchronous
    if (synchronous && response.result != null) {
        if (response.result.code != 0) {
            throw ProtocolError.TransactionFailed(
                response.result.log ?: "Transaction failed",
                response.txHash.value,
            )
        }
    }

    return HexString(response.txHash.value)
}

/**
 * Builds an unsigned transaction.
 *
 * @param payload The action payload (Base64 encoded)
 * @param description Transaction description
 * @param nonce Account nonce
 * @param signer The signer (for identifier and signature type)
 * @return Unsigned transaction
 */
private fun KwilProtocol.buildTransaction(
    payload: Base64String,
    description: String,
    nonce: Int,
    signer: Signer,
): TransactionBase64 =
    Transaction(
        signature = Signature(sig = null, type = signer.getSignatureType()),
        body =
            TxBody(
                desc = description,
                payload = payload,
                type = PayloadType.EXECUTE_ACTION,
                fee = "0",
                nonce = nonce,
                chainId = this.chainId,
            ),
        sender = signer.getIdentifier(),
        serialization = SerializationType.SIGNED_MSG_CONCAT,
    )

/**
 * Signs a transaction using KWIL's signature scheme.
 *
 * The signature message format:
 * ```
 * {description}
 *
 * PayloadType: {type}
 * PayloadDigest: {first 20 bytes of SHA-256 hash of payload}
 * Fee: {fee}
 * Nonce: {nonce}
 *
 * Kwil Chain ID: {chainId}
 * ```
 *
 * @param tx The unsigned transaction
 * @param signer The cryptographic signer
 * @return Signed transaction
 */
@OptIn(ExperimentalEncodingApi::class)
private suspend fun signTransaction(
    tx: TransactionBase64,
    signer: Signer,
): TransactionBase64 {
    val payload = requireNotNull(tx.body.payload) { "Payload is required" }

    // Decode Base64 payload to bytes and create digest (first 20 bytes of SHA-256 hash)
    val payloadBytes = Base64.decode(payload.value)
    val digest = SHA256().digest(payloadBytes).copyOfRange(0, 20)

    // Format the signature message (KWIL protocol specification)
    // Note: No preceding or succeeding whitespace allowed - exact format required
    val json = Json { encodeDefaults = false }
    val payloadTypeStr = json.encodeToString(PayloadType.serializer(), tx.body.type).replace("\"", "")

    val signatureMessage =
        buildString {
            append("${tx.body.desc}\n\n")
            append("PayloadType: $payloadTypeStr\n")
            append("PayloadDigest: ${HexString(digest).value}\n")
            append("Fee: ${tx.body.fee}\n")
            append("Nonce: ${tx.body.nonce}\n\n")
            append("Kwil Chain ID: ${tx.body.chainId}\n")
        }

    // Sign the formatted message
    val signatureBytes = signer.sign(signatureMessage.toByteArray())
    val signatureBase64 = Base64String(Base64.encode(signatureBytes))

    // Return transaction with signature
    return Transaction(
        signature = Signature(sig = signatureBase64, type = signer.getSignatureType()),
        body = tx.body,
        sender = tx.sender,
        serialization = tx.serialization,
    )
}
