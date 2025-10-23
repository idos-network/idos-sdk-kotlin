package org.idos.interop

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

/**
 * iOS interop extensions for ByteArray <-> NSData conversions.
 *
 * These extensions provide seamless conversion between Kotlin ByteArray
 * and Swift Data/NSData types. SKIE automatically handles these conversions
 * in most cases, but these extensions provide explicit control when needed.
 *
 * Usage in Swift:
 * ```swift
 * // Kotlin -> Swift (automatic via SKIE)
 * let data: Data = kotlinByteArray.toNSData()
 *
 * // Swift -> Kotlin (automatic via SKIE)
 * let byteArray: KotlinByteArray = data.toKotlinByteArray()
 * ```
 */

@Suppress("ktlint:standard:no-consecutive-comments")
/**
 * Converts a Kotlin ByteArray to NSData.
 *
 * This creates a copy of the data to ensure memory safety across
 * the Kotlin/Native to Objective-C boundary.
 *
 * @return NSData containing a copy of this ByteArray
 */
@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), this.size.toULong())
    }

/**
 * Converts NSData to a Kotlin ByteArray.
 *
 * This creates a copy of the data to ensure memory safety across
 * the Objective-C to Kotlin/Native boundary.
 *
 * @return ByteArray containing a copy of this NSData
 */
@OptIn(ExperimentalForeignApi::class)
fun NSData.toKotlinByteArray(): ByteArray {
    val length = this.length.toInt()
    val byteArray = ByteArray(length)
    if (length > 0) {
        byteArray.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), this.bytes, length.toULong())
        }
    }
    return byteArray
}
