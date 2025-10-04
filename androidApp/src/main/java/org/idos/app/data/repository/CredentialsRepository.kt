package org.idos.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.idos.app.data.DataProvider
import org.idos.app.data.model.Credential
import org.idos.app.data.model.CredentialDetail
import org.idos.app.data.model.Notes
import org.idos.kwil.domain.generated.view.GetCredentialOwnedResponse
import org.idos.kwil.domain.generated.view.GetCredentialsResponse
import org.idos.kwil.types.UuidString

interface CredentialsRepository {
    fun getCredentials(): Flow<List<Credential>>

    fun getCredential(id: org.idos.kwil.types.UuidString): Flow<CredentialDetail>
}

class CredentialsRepositoryImpl(
    private val dataProvider: DataProvider,
) : CredentialsRepository {
    override fun getCredentials(): Flow<List<Credential>> =
        flow {
            emit(
                dataProvider
                    .getCredentials()
                    .getOrThrow()
                    .filter { it.publicNotes.isNotBlank() }
                    .map { it.toCredential() },
            )
        }

    override fun getCredential(id: org.idos.kwil.types.UuidString): Flow<CredentialDetail> =
        flow {
            val credential = dataProvider.getCredential(id).getOrThrow()
            emit(credential.toDetail())
        }
}

@Serializable
data class PublicNotes(
    @SerialName("id") val id: org.idos.kwil.types.UuidString,
    @SerialName("type") val type: String,
    @SerialName("level") val level: String,
    @SerialName("status") val status: String,
    @SerialName("issuer") val issuer: String,
)

fun org.idos.kwil.domain.generated.view.GetCredentialsResponse.toCredential(): Credential {
    val publicNotes = Json.decodeFromString<PublicNotes>(this.publicNotes)
    return Credential(
        id,
        notes =
            Notes(
                publicNotes.id,
                publicNotes.type,
                publicNotes.level,
                publicNotes.status,
                publicNotes.issuer,
            ),
    )
}

fun org.idos.kwil.domain.generated.view.GetCredentialOwnedResponse.toDetail(): CredentialDetail =
    CredentialDetail(id, content, encryptorPublicKey)
