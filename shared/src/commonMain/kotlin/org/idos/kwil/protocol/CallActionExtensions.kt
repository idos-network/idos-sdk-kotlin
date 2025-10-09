package org.idos.kwil.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.idos.kwil.domain.Empty
import org.idos.kwil.domain.NoParamsAction
import org.idos.kwil.domain.ViewAction
import org.idos.kwil.security.signer.Signer
import org.idos.kwil.serialization.toMessage

// Extension functions for KwilProtocol to execute view actions (read-only queries).
//
// View actions don't modify database state and return query results.

/**
 * Executes a view action (read-only query).
 *
 * @param action The view action descriptor
 * @param input Action input parameters
 * @param signer Optional signer for authenticated calls
 * @return List of result rows
 */
suspend inline fun <I, reified O> KwilProtocol.callAction(
    action: ViewAction<I, O>,
    input: I,
    signer: Signer? = null,
): List<O> {
    val message = action.toMessage(input, signer)
    val response = callMethod(message)
    return parseQueryResponse(response.queryResult)
}

/**
 * Executes a view action with no parameters.
 *
 * @param action The no-params view action descriptor
 * @param signer Optional signer for authenticated calls
 * @return List of result rows
 */
suspend inline fun <reified O> KwilProtocol.callAction(
    action: NoParamsAction<O>,
    signer: Signer? = null,
): List<O> = callAction(action, Empty, signer)

/**
 * Parses a QueryResponse into typed result objects.
 *
 * @param queryResponse The raw query response
 * @param json JSON configuration for deserialization
 * @return List of typed result objects
 */
inline fun <reified T> parseQueryResponse(
    queryResponse: QueryResponse,
    json: Json = Json { ignoreUnknownKeys = true },
): List<T> {
    val columnNames = queryResponse.columnNames ?: emptyList()
    val values = queryResponse.values ?: emptyList()

    return values.map { row ->
        val obj =
            buildJsonObject {
                for ((col, value) in columnNames.zip(row)) {
                    put(col, value)
                }
            }
        json.decodeFromJsonElement<T>(obj)
    }
}
