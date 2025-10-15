package org.idos.enclave.mpc

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.core.readBytes
import kotlinx.io.Buffer
import kotlinx.io.readIntLe
import kotlinx.io.readString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.idos.kwil.serialization.toByteArray
import org.idos.kwil.types.Base64String
import org.idos.kwil.types.HexString

@Serializable
data class ContractState(
    val address: String,
    val serializedContract: Base64String,
)

data class NodeConfig(
    val address: HexString,
    val url: String,
)

internal class PartisiaRpcClient(
    val url: String,
    val contract: String,
) {
    companion object {
        internal fun readContractState(state: Base64String): List<NodeConfig> {
            val buf = Buffer().apply { write(state.toByteArray()) }
            val len = buf.readIntLe()
            val nodes = mutableListOf<NodeConfig>()
            repeat(len) {
                val address = buf.readBytes(21).toHexString()
                val count = buf.readIntLe().toLong()
                val node = buf.readString(count)
                nodes.add(NodeConfig(address, node))
            }
            return nodes
        }
    }

    private val httpClient =
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

    private suspend fun getContract(): ContractState {
        val url = "$url/chain/contracts/$contract"
        val response =
            httpClient.get(url)
        val state = response.body<ContractState>()
        return state
    }

    suspend fun getState(): List<NodeConfig> {
        val contract = getContract()
        val nodes = readContractState(contract.serializedContract)

        return nodes
    }
}
