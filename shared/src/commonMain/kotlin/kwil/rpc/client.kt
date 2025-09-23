package org.idos.kwil.rpc

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.concurrent.atomics.AtomicInt
import kotlinx.serialization.json.Json
import org.idos.kwil.signer.SignatureType

/** https://github.com/trufnetwork/kwil-js/blob/main/src/api_client/client.ts */
class MissingAuthenticationException : Exception()

@OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)
open class ApiClient(val baseUrl: String = "https://nodes.staging.idos.network") {
    protected var cookie: String? = null
    protected var unconfirmedNonce: Boolean = false

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    }
            )
        }
    }

    private val requestIdCounter = AtomicInt(1)

    private suspend inline fun <reified TRequest> doRequest(
            method: JSONRPCMethod,
            params: TRequest
    ): HttpResponse {
        val request =
                JsonRPCRequest(
                        id = requestIdCounter.fetchAndAdd(1),
                        method = method.value,
                        jsonrpc = "2.0",
                        params = params
                )

        val requestCookie = this.cookie ?: ""

        return httpClient.post("$baseUrl/rpc/v1") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            headers { append(HttpHeaders.Cookie, requestCookie) }
            setBody(request)
        }
    }

    private suspend inline fun <reified TRequest, reified TResponse> call(
            method: JSONRPCMethod,
            params: TRequest
    ): TResponse {
        val response = this.doRequest(method, params)

        val jsonRpcResponse = response.body<JsonRPCResponse<TResponse>>()

        if (jsonRpcResponse.error != null) {
            // https://github.com/trufnetwork/kwil-js/blob/main/src/core/enums.ts#L83
            // -901 means KGW mode
            if (jsonRpcResponse.error.code == -901) {
                throw MissingAuthenticationException()
            }

            throw RuntimeException("RPC Error: ${jsonRpcResponse.error.message}")
        }

        return jsonRpcResponse.result ?: throw RuntimeException("No result in response")
    }

    suspend fun ping(message: String): PingResponse =
            call(JSONRPCMethod.METHOD_PING, PingRequest(message))

    suspend fun chainInfo(): ChainInfoResponse =
            call(JSONRPCMethod.METHOD_CHAIN_INFO, ChainInfoRequest())

    suspend fun health(): HealthResponse = call(JSONRPCMethod.METHOD_HEALTH, HealthRequest())

    // https://github.com/trufnetwork/kwil-js/blob/main/src/api_client/client.ts#L143C19-L143C35
    suspend fun getAccountClient(id: AccountId): AccountResponse =
            call(
                    JSONRPCMethod.METHOD_ACCOUNT,
                    AccountRequest(
                            id,
                            if (this.unconfirmedNonce) AccountStatus.PENDING
                            else AccountStatus.LATEST
                    )
            )

    //
    // https://github.com/trufnetwork/kwil-js/blob/main/src/api_client/client.ts#L190
    //
    suspend fun broadcastClient(
            tx: TransactionBase64,
            sync: BroadcastSyncType? = null
    ): BroadcastResponse {
        if (!tx.isSigned()) {
            throw IllegalStateException("Transaction must be signed first.")
        }

        return call<BroadcastRequest, BroadcastResponse>(
                method = JSONRPCMethod.METHOD_BROADCAST,
                params =
                        BroadcastRequest(
                                tx = tx,
                                sync = sync,
                        )
        )
    }

    suspend fun callMethod(msg: Message): CallResponse = call(JSONRPCMethod.METHOD_CALL, msg)

    suspend fun listDatabases(owner: HexString? = null): ListDatabasesResponse =
            call(JSONRPCMethod.METHOD_DATABASES, ListDatabasesRequest(owner))

    suspend fun estimateCostClient(tx: TransactionBase64): EstimatePriceResponse =
            call(JSONRPCMethod.METHOD_PRICE, EstimatePriceRequest(tx))

    suspend fun query(
            query: String,
            params: Map<String, kotlinx.serialization.json.JsonElement>
    ): QueryResponse = call(JSONRPCMethod.METHOD_QUERY, SelectQueryRequest(query, params))

    suspend fun txQuery(txHash: String): TxQueryResponse =
            call(JSONRPCMethod.METHOD_TX_QUERY, TxQueryRequest(txHash))

    suspend fun schema(namespace: String): SchemaResponse =
            call(JSONRPCMethod.METHOD_SCHEMA, SchemaRequest(namespace))

    suspend fun challenge(): ChallengeResponse =
            call(JSONRPCMethod.METHOD_CHALLENGE, ChallengeRequest())

    suspend fun authParam(): KGWAuthInfo = call(JSONRPCMethod.METHOD_KGW_PARAM, AuthParamRequest())

    suspend fun authn(
            nonce: String,
            sender: HexString,
            signature: Base64String,
            signatureType: SignatureType
    ): String? {
        val response =
                this.doRequest(
                        JSONRPCMethod.METHOD_KGW_AUTHN,
                        AuthnRequest(nonce, sender, Signature(signature, signatureType))
                )
        return response.headers["Set-Cookie"]?.split(";")?.get(0)
    }

    suspend fun logout(account: Base64String): Unit =
            call(JSONRPCMethod.METHOD_KGW_LOGOUT, AuthnLogoutRequest(account))
}
