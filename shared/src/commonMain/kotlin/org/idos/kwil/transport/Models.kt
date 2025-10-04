package org.idos.kwil.transport

import kotlinx.serialization.Serializable

/**
 * JSON-RPC 2.0 request.
 *
 * See: https://www.jsonrpc.org/specification
 */
@Serializable
data class JsonRpcRequest<T>(
    val jsonrpc: String = "2.0",
    val id: Int,
    val method: String,
    val params: T,
)

/**
 * JSON-RPC 2.0 response.
 *
 * See: https://www.jsonrpc.org/specification
 */
@Serializable
data class JsonRpcResponse<T>(
    val jsonrpc: String,
    val id: Int,
    val result: T? = null,
    val error: JsonRpcError? = null,
)

/**
 * JSON-RPC 2.0 error object.
 *
 * See: https://www.jsonrpc.org/specification#error_object
 */
@Serializable
data class JsonRpcError(
    val code: Int? = null,
    val message: String? = null,
    val data: kotlinx.serialization.json.JsonElement? = null,
)
