package org.idos.app.data.model

data class Credential(
    val type: String,
    val level: String,
    val status: String,
    val issuer: String,
    val shares: Int
)

data class Wallet(
    val address: String,
    val network: String
)
