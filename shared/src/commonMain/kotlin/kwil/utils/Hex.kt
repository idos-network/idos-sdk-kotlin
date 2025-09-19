package org.idos.kwil.utils

import org.idos.kwil.rpc.HexString

// https://github.com/trufnetwork/kwil-js/blob/main/src/utils/serial.ts#L82C17-L82C27
fun bytesToHex(bytes: ByteArray): HexString = bytes.toHexString()

fun hexToBytes(hex: String): ByteArray = hex.removePrefix("0x").hexToByteArray()