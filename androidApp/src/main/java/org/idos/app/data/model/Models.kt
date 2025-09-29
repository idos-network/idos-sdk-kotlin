package org.idos.app.data.model

import kotlinx.serialization.Serializable
import org.idos.kwil.rpc.HexString
import org.idos.kwil.rpc.UuidString

data class Credential(
    val id: UuidString,
    val notes: Notes,
)

data class Notes(
    val id: UuidString,
    val type: String,
    val level: String,
    val status: String,
    val issuer: String,
)

data class CredentialDetail(
    val id: UuidString,
    val content: String,
    val encryptorPublicKey: String,
)

data class Wallet(
    val address: String,
    val network: String,
)

@Serializable
data class UserModel(
    val id: UuidString,
    val walletAddress: HexString,
    val lastUpdated: Long = System.currentTimeMillis(),
)
