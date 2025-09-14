package kwil.utils

import java.security.MessageDigest

fun sha256BytesToBytes(message: ByteArray): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(message)
}
