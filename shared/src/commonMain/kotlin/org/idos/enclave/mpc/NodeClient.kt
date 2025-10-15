package org.idos.enclave.mpc

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.idos.kwil.types.HexString

@Serializable
data class Sharing(
    val shareCommitments: List<String>,
    val recoveringAddresses: List<String>,
    val shareData: String,
)

@Serializable
data class EncryptedShare(
    val encryptedShare: String,
    val publicKey: String,
    val nonce: String,
)

/**
 * Client for interacting with individual MPC node engines.
 * Handles upload, download, and management of encrypted shares.
 *
 * Pure HTTP client - authentication is handled externally via pre-signed signatures.
 */
internal class NodeClient(
    private val baseUrl: String,
    private val contractAddress: HexString,
) {
    @OptIn(ExperimentalSerializationApi::class)
    private val httpClient =
        HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = false
                        namingStrategy = JsonNamingStrategy.SnakeCase
                    },
                )
            }
        }

    /**
     * Upload a share to the offchain storage.
     *
     * @param id The share identifier
     * @param uploadRequest The sharing data to upload
     * @param signature Pre-signed authorization signature (e.g., "0x...")
     * @return HTTP status code as string
     */
    suspend fun sendUpload(
        id: String,
        uploadRequest: Sharing,
        signature: String,
    ): String =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/offchain/$contractAddress/shares/$id"
            val response: HttpResponse =
                httpClient.put(url) {
                    headers {
                        append("Authorization", signature)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(uploadRequest)
                }

            if (response.status.value != 201) {
                throw Exception("Error uploading share to $contractAddress at $url: ${response.status}")
            }

            response.status.value.toString()
        }

    /**
     * Download an encrypted share from the offchain storage.
     *
     * @param id The share identifier
     * @param downloadRequest The download request parameters
     * @param signature Pre-signed authorization signature (e.g., "0x...")
     * @return The encrypted share data
     */
    suspend fun sendDownload(
        id: String,
        downloadRequest: DownloadRequest,
        signature: String,
    ): EncryptedShare =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/offchain/$contractAddress/shares/$id"
            val response: HttpResponse =
                httpClient.post(url) {
                    headers {
                        append("Authorization", signature)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(downloadRequest)
                }

            if (response.status.value != 200) {
                throw Exception("Error downloading share from $contractAddress at $url: ${response.status}: ${response.bodyAsText()}")
            }

            response.body<EncryptedShare>()
        }

    /**
     * Update wallet information for a share.
     *
     * @param id The share identifier
     * @param updateRequest The wallet update request
     * @param signature Pre-signed authorization signature (e.g., "0x...")
     * @return HTTP status code as string
     */
    suspend fun sendUpdate(
        id: String,
        updateRequest: UpdateWalletsRequest,
        signature: String,
    ): String =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/offchain/$contractAddress/shares/$id/wallets"
            val response: HttpResponse =
                httpClient.patch(url) {
                    headers {
                        append("Authorization", signature)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(updateRequest)
                }

            if (response.status.value != 200) {
                throw Exception("Error updating wallets for $contractAddress at $url: ${response.status}")
            }

            response.status.value.toString()
        }

    /**
     * Add a new address to a share.
     *
     * @param id The share identifier
     * @param addRequest The add address request
     * @param signature Pre-signed authorization signature (e.g., "0x...")
     * @return HTTP status code as string
     */
    suspend fun sendAddAddress(
        id: String,
        addRequest: AddAddressRequest,
        signature: String,
    ): String =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/offchain/$contractAddress/shares/$id/addresses"
            val response: HttpResponse =
                httpClient.post(url) {
                    headers {
                        append("Authorization", signature)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(addRequest)
                }

            if (response.status.value != 200) {
                throw Exception("Error adding address for $contractAddress at $url: ${response.status}")
            }

            response.status.value.toString()
        }

    /**
     * Remove an address from a share.
     *
     * @param id The share identifier
     * @param removeRequest The remove address request
     * @param signature Pre-signed authorization signature (e.g., "0x...")
     * @return HTTP status code as string
     */
    suspend fun sendRemoveAddress(
        id: String,
        removeRequest: RemoveAddressRequest,
        signature: String,
    ): String =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/offchain/$contractAddress/shares/$id/addresses"
            val response: HttpResponse =
                httpClient.patch(url) {
                    headers {
                        append("Authorization", signature)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(removeRequest)
                }

            if (response.status.value != 200) {
                throw Exception("Error removing address for $contractAddress at $url: ${response.status}")
            }

            response.status.value.toString()
        }

    fun close() {
        httpClient.close()
    }
}
