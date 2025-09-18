package org.idos.kwil

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.idos.kwil.actions.ActionSchema
import org.idos.kwil.auth.Auth
import org.idos.kwil.rpc.Action
import org.idos.kwil.rpc.ApiClient
import org.idos.kwil.rpc.AuthenticationMode
import org.idos.kwil.rpc.BroadcastResponse
import org.idos.kwil.rpc.BroadcastSyncType
import org.idos.kwil.rpc.CallBody
import org.idos.kwil.rpc.CallResponse
import org.idos.kwil.rpc.Message
import org.idos.kwil.rpc.MissingAuthenticationException
import org.idos.kwil.rpc.QueryResponse
import org.idos.kwil.signer.BaseSigner
import org.idos.kwil.transaction.ActionBuilder
import org.idos.kwil.transaction.ActionOptions
import org.idos.kwil.transaction.NamedParams
import org.idos.kwil.transaction.PositionalParams
import org.idos.kwil.transaction.PositionalTypes
import org.idos.kwil.utils.EncodedParameterValue
import org.idos.kwil.utils.encodeParams

/*
 * https://github.com/trufnetwork/kwil-js/blob/main/src/client/kwil.ts
 * https://github.com/trufnetwork/kwil-js/blob/main/src/client/node/nodeKwil.ts
 */
class KwilActionClient(
    baseUrl: String,
    val signer: BaseSigner,
    val chainId: String,
) : ApiClient(baseUrl) {
    private val actionsCache = mutableMapOf<String, List<Action>>()
    private var authMode: AuthenticationMode? = null
    private var auth: Auth = Auth(this)

    /**
     * Calls an action on the kwil nodes. This similar to `GET` like request.
     *
     * https://github.com/idos-network/idos-sdk-js/blob/859f2c67ace11bd879583d8c0cbf0f50c5257ba9/packages/%40core/src/kwil-infra/create-kwil-client.ts#L68
     */
    suspend inline fun <reified TResponse> callAction(
        actionName: String,
        params: NamedParams,
    ): List<TResponse> {
        val response =
            call(
                CallBody(
                    namespace = "main",
                    name = actionName,
                    inputs = createActionInputs(actionName, params),
                    types = actionTypes(actionName),
                ),
                signer,
            )

        return parseQueryResponse(response.queryResult)
    }

    inline fun <reified TResponse> parseQueryResponse(
        queryResponse: QueryResponse,
        json: Json = Json { ignoreUnknownKeys = true },
    ): List<TResponse> {
        val columnNames = queryResponse.columnNames ?: emptyList()
        val values = queryResponse.values ?: emptyList()

        return values.map { row ->
            val obj =
                buildJsonObject {
                    for ((col, value) in columnNames.zip(row)) {
                        put(col, value)
                    }
                }
            json.decodeFromJsonElement<TResponse>(obj)
        }
    }

    /**
     * Executes an action on the kwil nodes. This similar to `POST` like request.
     *
     * https://github.com/idos-network/idos-sdk-js/blob/859f2c67ace11bd879583d8c0cbf0f50c5257ba9/packages/%40core/src/kwil-infra/create-kwil-client.ts#L86
     */
    suspend inline fun executeAction(
        actionName: String,
        params: NamedParams,
        description: String,
        synchronous: Boolean = true,
    ): String? {
        val response =
            execute(
                CallBody(
                    namespace = "main",
                    name = actionName,
                    inputs = createActionInputs(actionName, params),
                    types = actionTypes(actionName),
                ),
                signer,
                description,
                if (synchronous) BroadcastSyncType.COMMIT else BroadcastSyncType.SYNC,
            )

        return response.txHash
    }

    /**
     * Base call is the same call in node.
     * https://github.com/trufnetwork/kwil-js/blob/main/src/client/node/nodeKwil.ts#L24
     * https://github.com/trufnetwork/kwil-js/blob/main/src/client/kwil.ts#L460
     */
    suspend fun call(
        actionBody: CallBody,
        signer: BaseSigner,
    ): CallResponse {
        // Ensure auth mode is set
        this.ensureAuthenticationMode()

        if (this.authMode == AuthenticationMode.OPEN) {
            val message = buildMessage(actionBody, signer)

            try {
                return this.callMethod(message)
            } catch (e: MissingAuthenticationException) {
                // https://github.com/trufnetwork/kwil-js/blob/4ffabc8ef583f9b0b8e71abaa7e7527c5e4f5b85/src/client/kwil.ts#L486
                requireNotNull(this.signer)

                val authenticationCookie = auth.authenticateKGW(this.signer)

                // Cookie was returned
                if (authenticationCookie != null) {
                    this.cookie = authenticationCookie
                }

                return this.callMethod(message)
            }
        }

        error("AuthMode not supported")
    }

    // https://github.com/trufnetwork/kwil-js/blob/main/src/client/kwil.ts#L513C1-L522C4
    suspend fun ensureAuthenticationMode() {
        if (this.authMode == null) {
            val health = this.health()
            this.authMode = health.mode
        }
    }

    // https://github.com/trufnetwork/kwil-js/blob/main/src/client/kwil.ts#L536
    fun buildMessage(
        callBody: CallBody,
        signer: BaseSigner? = null,
        challenge: String? = null,
        signature: String? = null,
    ): Message {
        // Validate action name is present
        if (callBody.name.isEmpty()) {
            throw IllegalArgumentException("name is required in actionBody")
        }

        requireNotNull(this.authMode) { "authMode is required" }

        // https://github.com/trufnetwork/kwil-js/blob/main/src/client/kwil.ts#L560
        val actionBuilder =
            ActionBuilder(
                kwil = this,
                options =
                    ActionOptions(
                        actionName = callBody.name.lowercase(),
                        namespace = callBody.namespace,
                        // https://github.com/trufnetwork/kwil-js/blob/main/src/client/kwil.ts#L549
                        actionInputs = listOf(callBody.inputs),
                        types = callBody.types,
                        chainId = this.chainId,
                    ),
            )

        /**
         * PUBLIC MODE
         * include the sender when the user passes a KwilSigner to kwil.call().
         * This is because the sender is required for queries that use @caller
         *
         */
        if (signer != null && this.authMode == AuthenticationMode.OPEN) {
            actionBuilder.addSigner(signer)
        }

        /**
         * PRIVATE MODE
         * include the sender when the user passes a KwilSigner to kwil.call().
         * only AFTER a challenge and signature is attached to the message
         *
         */
        if (signer != null && this.authMode === AuthenticationMode.PRIVATE) {
            if (challenge != null && signature != null) {
                // add challenge and signature to the message
                actionBuilder.challenge = challenge
                actionBuilder.signature = signature
                actionBuilder.addSigner(signer, signature, challenge)
            }
        }

        return actionBuilder.buildMsg(this.authMode === AuthenticationMode.PRIVATE)
    }

    /**
     * Executes a transaction on a Kwil network. These are mutative actions that must be mined on the Kwil network's blockchain.
     *
     * https://github.com/trufnetwork/kwil-js/blob/main/src/client/kwil.ts#L151
     */
    suspend fun execute(
        actionBody: CallBody,
        signer: BaseSigner,
        description: String,
        sync: BroadcastSyncType = BroadcastSyncType.COMMIT,
    ): BroadcastResponse {
        // Ensure auth mode is set
        this.ensureAuthenticationMode()

        // We don't need to call `resolveNamespace` since our code has no dbId!
        requireNotNull(actionBody.namespace)

        val actionBuilder =
            ActionBuilder(
                kwil = this,
                options =
                    ActionOptions(
                        actionName = actionBody.name.lowercase(),
                        namespace = actionBody.namespace,
                        description = description,
                        // https://github.com/trufnetwork/kwil-js/blob/main/src/client/kwil.ts#L167
                        actionInputs = listOf(actionBody.inputs),
                        types = actionBody.types,
                        chainId = this.chainId,
                    ),
            )

        actionBuilder.addSigner(signer)

        val transaction = actionBuilder.buildTx(this.authMode === AuthenticationMode.PRIVATE)

        val response = broadcastClient(transaction, sync)

        return response
    }

    suspend fun getActions(namespace: String): List<Action> =
        actionsCache.getOrPut(namespace) {
            this.selectQuery(
                "SELECT * FROM info.actions WHERE namespace = \$namespace",
                mapOf("\$namespace" to namespace),
            ) { response ->
                val values = response.values ?: emptyList()
                values.map { row ->
                    Action(
                        // TODO: Fix this
                        name = row[0].toString(),
                        parameters = emptyList(),
                        namespace = "main",
                        public = false,
                        modifiers = emptyList(),
                        body = null,
                    )
                }
            }
        }

    /**
     * https://github.com/trufnetwork/kwil-js/blob/main/src/client/kwil.ts#L218
     */
    suspend fun <T> selectQuery(
        query: String,
        params: Map<String, Any?> = emptyMap(),
        transform: (QueryResponse) -> T,
    ): T {
        val encodedParams = encodeParams(params)
        val jsonParams =
            encodedParams.mapValues { (_, encodedValue) ->
                Json.Default.encodeToJsonElement(EncodedParameterValue.serializer(), encodedValue)
            }
        val result = query(query, jsonParams)
        return transform(result)
    }

    /**
     * Creates action inputs as a positional array based on the action schema
     * https://github.com/idos-network/idos-sdk-js/blob/859f2c67ace11bd879583d8c0cbf0f50c5257ba9/packages/%40core/src/kwil-infra/create-kwil-client.ts#L75
     */
    fun createActionInputs(
        actionName: String,
        params: NamedParams = emptyMap(),
    ): PositionalParams {
        if (params.isEmpty()) {
            return emptyList()
        }

        val schema = ActionSchema.getSchema(actionName) ?: return emptyList()

        return schema.map { field ->
            when (val value = params[field.name]) {
                null -> null
                false, 0, "" -> value
                else -> value
            }
        }
    }

    /**
     * Gets the parameter types for an action based on its schema
     * https://github.com/idos-network/idos-sdk-js/blob/859f2c67ace11bd879583d8c0cbf0f50c5257ba9/packages/%40core/src/kwil-infra/create-kwil-client.ts#L75
     */
    fun actionTypes(actionName: String): PositionalTypes = ActionSchema.getSchema(actionName)?.map { it -> it.type } ?: emptyList()
}
