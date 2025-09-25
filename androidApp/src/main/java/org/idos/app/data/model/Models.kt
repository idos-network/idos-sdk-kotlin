package org.idos.app.data.model

import org.idos.kwil.rpc.UuidString

data class Credential(
    val id: UuidString,
    val type: String,
    val level: String,
    val status: String,
    val issuer: String,
)

data class Wallet(
    val address: String,
    val network: String,
)
