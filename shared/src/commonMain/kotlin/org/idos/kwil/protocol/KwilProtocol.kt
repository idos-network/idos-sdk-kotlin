package org.idos.kwil.protocol

import io.ktor.client.statement.HttpResponse
import org.idos.kwil.protocol.Message
import org.idos.kwil.protocol.TransactionBase64
import org.idos.kwil.security.auth.Auth
import org.idos.kwil.security.signer.SignatureType
import org.idos.kwil.transport.JsonRpcClient
import org.idos.kwil.transport.TransportError
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.HexString

/**
 * KWIL protocol client (L1 - Protocol Layer).
 *
 * Implements KWIL-specific RPC methods on top of the generic JSON-RPC transport.
 * Handles KWIL authentication, error codes, and protocol-specific logic.
 *
 * @param baseUrl KWIL network URL
 * @param chainId Chain identifier
 */
class KwilProtocol(
    val baseUrl: String,
    val chainId: String,
) {
    private val auth: Auth = Auth(this, KwilConstants.AUTH_VERSION)

    private val jsonRpcClient =
        JsonRpcClient(baseUrl) {
            auth.applyAuth(this)
        }

    /**
     * Generic RPC call with KWIL-specific error handling.
     *
     * @throws MissingAuthenticationException when KWIL Gateway requires auth (-901 error)
     * @throws ProtocolError.RpcError for other KWIL RPC errors
     */
    private suspend inline fun <reified TRequest, reified TResponse> call(
        method: JSONRPCMethod,
        params: TRequest,
    ): TResponse =
        try {
            jsonRpcClient.call(method.value, params)
        } catch (e: TransportError.HttpError) {
            // KWIL-specific error code handling
            if (e.statusCode == KwilConstants.KGW_AUTH_REQUIRED_CODE) {
                throw MissingAuthenticationException()
            }
            throw ProtocolError.RpcError(e.statusCode, e.message ?: "RPC error")
        }

    /**
     * Raw request for special cases (like auth that returns HttpResponse).
     */
    private suspend inline fun <reified TRequest> doRequest(
        method: JSONRPCMethod,
        params: TRequest,
    ): HttpResponse = jsonRpcClient.doRequest(method.value, params)

    // ========================================================================
    // KWIL RPC Methods
    // ========================================================================

    suspend fun ping(message: String): PingResponse = call(JSONRPCMethod.METHOD_PING, PingRequest(message))

    suspend fun chainInfo(): ChainInfoResponse = call(JSONRPCMethod.METHOD_CHAIN_INFO, Unit)

    suspend fun health(): HealthResponse = call(JSONRPCMethod.METHOD_HEALTH, Unit)

    suspend fun getAccount(
        id: AccountId,
        status: AccountStatus = AccountStatus.LATEST,
    ): AccountResponse =
        call(
            JSONRPCMethod.METHOD_ACCOUNT,
            AccountRequest(id, status),
        )

    suspend fun broadcast(
        tx: TransactionBase64,
        sync: BroadcastSyncType? = null,
    ): BroadcastResponse {
        if (!tx.isSigned()) {
            throw ProtocolError.InvalidResponse("Transaction must be signed first")
        }

        return call(
            JSONRPCMethod.METHOD_BROADCAST,
            BroadcastRequest(tx, sync),
        )
    }

    suspend fun callMethod(msg: Message): CallResponse = call(JSONRPCMethod.METHOD_CALL, msg)

    suspend fun listDatabases(owner: HexString? = null): ListDatabasesResponse =
        call(JSONRPCMethod.METHOD_DATABASES, ListDatabasesRequest(owner))

    suspend fun estimateCost(tx: TransactionBase64): EstimatePriceResponse = call(JSONRPCMethod.METHOD_PRICE, EstimatePriceRequest(tx))

    suspend fun query(
        query: String,
        params: Map<String, kotlinx.serialization.json.JsonElement>,
    ): QueryResponse = call(JSONRPCMethod.METHOD_QUERY, SelectQueryRequest(query, params))

    suspend fun txQuery(txHash: String): TxQueryResponse = call(JSONRPCMethod.METHOD_TX_QUERY, TxQueryRequest(txHash))

    suspend fun schema(namespace: String): SchemaResponse = call(JSONRPCMethod.METHOD_SCHEMA, SchemaRequest(namespace))

    suspend fun challenge(): ChallengeResponse = call(JSONRPCMethod.METHOD_CHALLENGE, Unit)

    suspend fun authParam(): KGWAuthInfo = call(JSONRPCMethod.METHOD_KGW_PARAM, Unit)

    suspend fun authn(
        nonce: String,
        sender: HexString,
        signature: Base64String,
        signatureType: SignatureType,
    ): HttpResponse =
        doRequest(
            JSONRPCMethod.METHOD_KGW_AUTHN,
            AuthnRequest(nonce, sender, Signature(signature, signatureType)),
        )

    suspend fun logout(account: Base64String): Unit = call(JSONRPCMethod.METHOD_KGW_LOGOUT, AuthnLogoutRequest(account))

    /**
     * Authenticates with KWIL Gateway using the provided signer.
     *
     * This obtains a session cookie that will be automatically attached to subsequent requests.
     * Called automatically when a MissingAuthenticationException is encountered.
     *
     * @param signer The cryptographic signer to use for authentication
     */
    suspend fun authenticate(signer: org.idos.kwil.security.signer.Signer) {
        auth.authenticateKGW(signer)
    }

    /**
     * Closes the underlying HTTP client.
     */
    fun close() {
        jsonRpcClient.close()
    }
}

/**
 * Exception thrown when KWIL Gateway requires authentication.
 *
 * This is converted from the -901 error code returned by KWIL Gateway.
 */
class MissingAuthenticationException : Exception("KWIL Gateway authentication required")
