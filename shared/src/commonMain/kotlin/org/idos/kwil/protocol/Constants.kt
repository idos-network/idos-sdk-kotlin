package org.idos.kwil.protocol

/**
 * KWIL protocol constants.
 */
internal object KwilConstants {
    /** JSON-RPC version */
    const val JSON_RPC_VERSION = "2.0"

    /** KWIL RPC endpoint path */
    const val RPC_ENDPOINT = "/rpc/v1"

    /** KGW authentication required error code */
    const val KGW_AUTH_REQUIRED_CODE = -901

    /** Default authentication version */
    const val AUTH_VERSION = "1"
}
