package org.idos.app.data.model

import kotlinx.serialization.Serializable
import org.idos.kwil.types.HexString
import org.idos.kwil.types.UuidString

data class Credential(
    val id: org.idos.kwil.types.UuidString,
    val notes: Notes,
)

data class Notes(
    val id: org.idos.kwil.types.UuidString,
    val type: String,
    val level: String,
    val status: String,
    val issuer: String,
)

data class CredentialDetail(
    val id: org.idos.kwil.types.UuidString,
    val content: String,
    val encryptorPublicKey: String,
)

data class Wallet(
    val address: String,
    val network: String,
)

@Serializable
data class UserModel(
    val id: org.idos.kwil.types.UuidString,
    val walletAddress: org.idos.kwil.types.HexString,
    val lastUpdated: Long = System.currentTimeMillis(),
)
