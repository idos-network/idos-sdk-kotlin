package org.idos.kwil.transport

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.concurrent.atomics.AtomicInt

/**
 * Pure JSON-RPC 2.0 transport client over HTTP.
 *
 * This is a generic JSON-RPC client with no KWIL-specific logic.
 * Handles HTTP communication, request ID generation, and JSON serialization.
 *
 * @param baseUrl The base URL for the JSON-RPC endpoint
 * @param requestInterceptor Optional interceptor to modify requests (e.g., add auth headers)
 */
@OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)
internal class JsonRpcClient(
    private val baseUrl: String,
    private val requestInterceptor: (HttpRequestBuilder.() -> Unit)? = null,
) {
    private val httpClient =
        HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    },
                )
            }
        }

    private val requestIdCounter = AtomicInt(1)

    /**
     * Executes a JSON-RPC request and returns the raw HTTP response.
     *
     * @param method JSON-RPC method name
     * @param params Request parameters
     * @return Raw HTTP response
     * @throws TransportError.NetworkError on network failures
     * @throws TransportError.Timeout on timeout
     */
    suspend inline fun <reified TRequest> doRequest(
        method: String,
        params: TRequest,
    ): HttpResponse {
        val request =
            JsonRpcRequest(
                id = requestIdCounter.fetchAndAdd(1),
                method = method,
                jsonrpc = JSON_RPC_VERSION,
                params = params,
            )

        return try {
            httpClient.post("$baseUrl$RPC_ENDPOINT") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(request)
                requestInterceptor?.invoke(this)
            }
        } catch (e: Exception) {
            throw TransportError.NetworkError(
                message = "Network request failed: ${e.message}",
                cause = e,
            )
        }
    }

    companion object {
        /** JSON-RPC version */
        private const val JSON_RPC_VERSION = "2.0"

        /** RPC endpoint path */
        private const val RPC_ENDPOINT = "/rpc/v1"
    }

    /**
     * Executes a JSON-RPC call and deserializes the response.
     *
     * @param method JSON-RPC method name
     * @param params Request parameters
     * @return Deserialized response result
     * @throws TransportError.NetworkError on network failures
     * @throws TransportError.SerializationError on JSON parsing errors
     * @throws TransportError.HttpError on JSON-RPC error responses
     */
    suspend inline fun <reified TRequest, reified TResponse> call(
        method: String,
        params: TRequest,
    ): TResponse {
        val httpResponse = doRequest(method, params)

        val jsonRpcResponse =
            try {
                httpResponse.body<JsonRpcResponse<TResponse>>()
            } catch (e: Exception) {
                throw TransportError.SerializationError(
                    message = "Failed to parse JSON-RPC response: ${e.message}",
                    cause = e,
                )
            }

        if (jsonRpcResponse.error != null) {
            val errorCode = jsonRpcResponse.error.code ?: -1
            val errorMessage = jsonRpcResponse.error.message ?: "Unknown error"

            throw TransportError.HttpError(
                statusCode = errorCode,
                message = "JSON-RPC error [$errorCode]: $errorMessage",
            )
        }

        return jsonRpcResponse.result
            ?: throw TransportError.SerializationError(
                message = "No result in JSON-RPC response",
                cause = IllegalStateException("Response contains neither result nor error"),
            )
    }

    /**
     * Closes the HTTP client and releases resources.
     */
    fun close() {
        httpClient.close()
    }
}
