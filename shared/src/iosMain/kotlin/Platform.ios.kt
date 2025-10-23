package org.idos

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

@OptIn(ExperimentalForeignApi::class)
class IosSecureRandom : SecureRandom {
    override fun nextInt(int: Int): Int {
        require(int > 0)
        val bytesNeeded = 4
        val byteArray = ByteArray(bytesNeeded)
        while (true) {
            byteArray.usePinned {
                val status = SecRandomCopyBytes(kSecRandomDefault, bytesNeeded.toULong(), it.addressOf(0))
                require(status == 0) { "Failed to get secure random bytes" }
            }
            val intVal = byteArray.fold(0) { acc, b -> (acc shl 8) or (b.toInt() and 0xFF) }
            val positiveVal = intVal ushr 1 // clear sign bit
            if (positiveVal < (Int.MAX_VALUE.toLong() / int) * int) {
                return (positiveVal % int)
            }
        }
    }
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getCurrentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun getSecureRandom(): SecureRandom = IosSecureRandom()
