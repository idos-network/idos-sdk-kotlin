package org.idos.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMap
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.idos.app.data.ApiClient
import org.idos.app.data.DataProvider
import org.idos.app.data.model.Credential
import org.idos.kwil.actions.generated.view.GetCredentialsResponse
import org.idos.kwil.rpc.UuidString

interface CredentialsRepository {
    fun getCredentials(): Flow<List<Credential>>
}

class CredentialsRepositoryImpl(
    private val dataProvider: DataProvider,
) : CredentialsRepository {
    override fun getCredentials(): Flow<List<Credential>> =
        flow {
            emit(
                dataProvider
                    .getCredentials()
                    .filter { it.publicNotes.isNotBlank() }
                    .map { it.toCredential() },
            )
        }
}

@Serializable
data class PublicNotes(
    @SerialName("id") val id: UuidString,
    @SerialName("type") val type: String,
    @SerialName("level") val level: String,
    @SerialName("status") val status: String,
    @SerialName("issuer") val issuer: String,
)

fun GetCredentialsResponse.toCredential(): Credential {
    val publicNotes = Json.decodeFromString<PublicNotes>(this.publicNotes)
    return Credential(
        publicNotes.id,
        publicNotes.type,
        publicNotes.level,
        publicNotes.status,
        publicNotes.issuer,
    )
}
