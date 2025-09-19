package org.idos.kwil.rpc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.idos.kwil.signer.KeyType
import org.idos.kwil.signer.SignatureType
import org.idos.kwil.transaction.AccessModifier
import org.idos.kwil.transaction.PositionalParams
import org.idos.kwil.transaction.PositionalTypes

// Base JSON-RPC types
@Serializable
data class JsonRPCRequest<T>(
    val jsonrpc: String = "2.0",
    val id: Int,
    val method: String,
    val params: T,
)

@Serializable
data class JsonRPCResponse<T>(
    val jsonrpc: String,
    val id: Int,
    val result: T? = null,
    val error: JsonRPCError? = null,
)

@Serializable
data class JsonRPCError(
    val code: Int? = null,
    val message: String? = null,
    val data: kotlinx.serialization.json.JsonElement? = null,
)

// https://github.com/trufnetwork/kwil-js/blob/4ffabc8ef583f9b0b8e71abaa7e7527c5e4f5b85/src/core/action.ts#L163
data class CallBody(
    val namespace: String,
    val name: String,
    val inputs: PositionalParams,
    val types: PositionalTypes,
)

// https://github.com/trufnetwork/kwil-js/blob/main/src/core/enums.ts#L67
@Serializable
enum class AuthenticationMode(
    val value: String,
) {
    @SerialName("private")
    PRIVATE(value = "private"),

    @SerialName("open")
    OPEN(value = "open"),
}

// Enums
@Serializable
enum class JSONRPCMethod(
    val value: String,
) {
    @SerialName("user.health")
    METHOD_HEALTH("user.health"),

    @SerialName("user.ping")
    METHOD_PING("user.ping"),

    @SerialName("user.chain_info")
    METHOD_CHAIN_INFO("user.chain_info"),

    @SerialName("user.account")
    METHOD_ACCOUNT("user.account"),

    @SerialName("user.broadcast")
    METHOD_BROADCAST("user.broadcast"),

    @SerialName("user.call")
    METHOD_CALL("user.call"),

    @SerialName("user.databases")
    METHOD_DATABASES("user.databases"),

    @SerialName("user.estimate_price")
    METHOD_PRICE("user.estimate_price"),

    @SerialName("user.query")
    METHOD_QUERY("user.query"),

    @SerialName("user.tx_query")
    METHOD_TX_QUERY("user.tx_query"),

    @SerialName("user.schema")
    METHOD_SCHEMA("user.schema"),

    @SerialName("kgw.authn_param")
    METHOD_KGW_PARAM("kgw.authn_param"),

    @SerialName("kgw.authn")
    METHOD_KGW_AUTHN("kgw.authn"),

    @SerialName("kgw.logout")
    METHOD_KGW_LOGOUT("kgw.logout"),

    @SerialName("user.challenge")
    METHOD_CHALLENGE("user.challenge"),
}

@Serializable(with = AccountStatusSerializer::class)
enum class AccountStatus(
    val value: Int,
) {
    LATEST(0),
    PENDING(1),
    ;

    companion object {
        fun fromInt(value: Int) =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown BroadcastSyncType: $value")
    }
}

object AccountStatusSerializer : KSerializer<AccountStatus> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AccountStatus", PrimitiveKind.INT)

    override fun serialize(
        encoder: Encoder,
        value: AccountStatus,
    ) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): AccountStatus {
        val intValue = decoder.decodeInt()
        return AccountStatus.fromInt(intValue)
    }
}

@Serializable(with = BroadcastSyncTypeSerializer::class)
enum class BroadcastSyncType(
    val value: Int,
) {
    SYNC(0),
    COMMIT(1),
    ;

    companion object {
        fun fromInt(value: Int) =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown BroadcastSyncType: $value")
    }
}

object BroadcastSyncTypeSerializer : KSerializer<BroadcastSyncType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BroadcastSyncType", PrimitiveKind.INT)

    override fun serialize(
        encoder: Encoder,
        value: BroadcastSyncType,
    ) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): BroadcastSyncType {
        val intValue = decoder.decodeInt()
        return BroadcastSyncType.fromInt(intValue)
    }
}

@Serializable
enum class BytesEncodingStatus {
    @SerialName("base64")
    BASE64_ENCODED,

    @SerialName("hex")
    HEX_ENCODED,

    @SerialName("raw")
    RAW,
}

@Serializable
enum class PayloadType {
    @SerialName("invalid")
    INVALID_PAYLOAD_TYPE,

    @SerialName("execute")
    EXECUTE_ACTION,

    @SerialName("call_action")
    CALL_ACTION,

    @SerialName("transfer")
    TRANSFER,

    @SerialName("raw_statement")
    RAW_STATEMENT,
}

@Serializable
data class AccountId(
    val identifier: HexString?,
    @SerialName("key_type")
    val keyType: KeyType,
)

// Request types
@Serializable
data class SchemaRequest(
    val namespace: String,
)

@Serializable
data class AccountRequest(
    val id: AccountId,
    val status: AccountStatus,
)

@Serializable
data class BroadcastRequest(
    val tx: TransactionBase64,
    val sync: BroadcastSyncType? = null,
)

@Serializable
class ChainInfoRequest

@Serializable
class ChallengeRequest

@Serializable
class HealthRequest

@Serializable
data class ListDatabasesRequest(
    val owner: HexString? = null,
)

@Serializable
data class PingRequest(
    val message: String,
)

@Serializable
data class EstimatePriceRequest(
    val tx: TransactionBase64,
)

@Serializable
data class SelectQueryRequest(
    val query: String,
    val params: Map<String, kotlinx.serialization.json.JsonElement>,
)

@Serializable
data class TxQueryRequest(
    @SerialName("tx_hash")
    val txHash: String,
)

@Serializable
class AuthParamRequest

@Serializable
data class AuthnRequest(
    val nonce: String,
    val sender: HexString,
    val signature: Signature,
)

@Serializable
data class Signature(
    val sig: Base64String?,
    val type: SignatureType,
)

@Serializable
data class AuthnLogoutRequest(
    val account: Base64String,
)

// Response types
@Serializable
data class SchemaResponse(
    val schema: DatabaseSchema,
)

@Serializable
data class DatabaseSchema(
    val owner: String,
    val name: String? = null,
    val tables: List<Table>? = null,
    val actions: List<Action>? = null,
    val extensions: List<Extension>? = null,
)

@Serializable
data class Table(
    val name: String,
    val columns: List<Column>,
)

@Serializable
data class Column(
    val name: String,
    val type: String,
    @SerialName("is_primary")
    val isPrimary: Boolean = false,
    @SerialName("is_not_null")
    val isNotNull: Boolean = false,
)

@Serializable
data class Action(
    val name: String,
    val namespace: String,
    val parameters: List<String>? = null,
    val public: Boolean = false,
    val modifiers: List<AccessModifier>? = null,
    val body: String? = null,
)

@Serializable
data class Extension(
    val name: String,
    val initialization: List<kotlinx.serialization.json.JsonElement>? = null,
    val alias: String? = null,
)

@Serializable
data class AccountResponse(
    val id: AccountId? = null,
    val balance: String,
    val nonce: Int,
)

@Serializable
data class BroadcastResponse(
    @SerialName("tx_hash")
    val txHash: Base64String,
    val result: BroadcastResult? = null,
)

@Serializable
data class BroadcastResult(
    val code: Int,
    val gas: Int,
    val log: String? = null,
    val events: List<kotlinx.serialization.json.JsonElement>? = null,
)

@Serializable
data class ChainInfoResponse(
    @SerialName("chain_id")
    val chainId: String,
    @SerialName("block_height")
    val blockHeight: Long,
    @SerialName("block_hash")
    val blockHash: String,
)

@Serializable
data class ChallengeResponse(
    val challenge: HexString,
)

@Serializable
data class HealthResponse(
    @SerialName("chain_id")
    val chainId: String,
    @SerialName("block_height")
    val blockHeight: Long,
    @SerialName("block_hash")
    val blockHash: String,
    val gas: Boolean,
    val healthy: Boolean,
    val version: String,
    @SerialName("block_time")
    val blockTime: Long,
    @SerialName("block_age")
    val blockAge: Long,
    val syncing: Boolean,
    val height: Long,
    @SerialName("app_hash")
    val appHash: HexString,
    @SerialName("peer_count")
    val peerCount: Int,
    val mode: AuthenticationMode,
)

@Serializable
data class QueryResponse(
    @SerialName("column_names")
    val columnNames: List<String>? = null,
    @SerialName("column_types")
    val columnTypes: List<ColumnType>? = null,
    val values: List<List<kotlinx.serialization.json.JsonElement>>? = null,
)

@Serializable
data class ColumnType(
    val name: String,
    @SerialName("is_array")
    val isArray: Boolean,
    val metadata: List<Int>,
)

@Serializable
data class CallResponse(
    @SerialName("query_result")
    val queryResult: QueryResponse,
    val logs: String? = null,
    val error: String? = null,
)

@Serializable
data class ListDatabasesResponse(
    val databases: List<DatasetInfoServer>? = null,
)

@Serializable
data class DatasetInfoServer(
    val name: String,
    val owner: String,
    @SerialName("dbid")
    val dbId: String,
)

@Serializable
data class PingResponse(
    val message: String,
)

@Serializable
data class EstimatePriceResponse(
    val price: String,
)

@Serializable
data class TxQueryResponse(
    @SerialName("tx_hash")
    val txHash: String,
    val height: Long,
    // TODO: val tx: TxnData,
    @SerialName("tx_result")
    val txResult: TxResult,
)

@Serializable
data class KGWAuthInfo(
    val nonce: String,
    val statement: String,
    @SerialName("issue_at")
    val issueAt: String,
    @SerialName("expiration_time")
    val expirationTime: String,
    @SerialName("chain_id")
    val chainId: String?,
    val domain: String?,
    val version: String?,
    val uri: String,
)

// Supporting data classes

@Serializable
data class TxResult(
    val code: Int,
    val data: String? = null,
    val log: String? = null,
    val info: String? = null,
    @SerialName("gas_wanted")
    val gasWanted: Long? = null,
    @SerialName("gas_used")
    val gasUsed: Long? = null,
    val events: List<kotlinx.serialization.json.JsonElement>? = null,
    val codespace: String? = null,
)
