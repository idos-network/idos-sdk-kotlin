package org.idos.kwil.protocol

import io.ktor.utils.io.core.toByteArray
import org.idos.kwil.domain.ExecuteAction
import org.idos.signer.Signer
import org.idos.kwil.serialization.toTransaction
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.HexString
import org.kotlincrypto.hash.sha2.SHA256
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// Extension functions for KwilProtocol to execute mutative actions (transactions).
//
// Execute actions modify database state and are mined on the blockchain.

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
    input: List<I>,
    signer: Signer,
    synchronous: Boolean = true,
): HexString {
    // 1. Get account to fetch nonce
    val account = getAccount(signer.accountId())
    val nonce = account.nonce + 1

    // 3. Build transaction
    val tx = action.toTransaction(input, signer, nonce, chainId)

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
                response.txHash,
            )
        }
    }

    return response.txHash
}

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
    val signatureMessage =
        buildString {
            append("${tx.body.desc}\n\n")
            append("PayloadType: ${tx.body.type.value}\n")
            append("PayloadDigest: ${digest.toHexString()}\n")
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
