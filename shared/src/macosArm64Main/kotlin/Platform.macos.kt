package org.idos

import platform.Foundation.NSDate
import platform.Foundation.NSProcessInfo
import platform.Foundation.timeIntervalSince1970

class MacosPlatform : Platform {
    override val name: String = "macOS ${NSProcessInfo.processInfo.operatingSystemVersionString}"
}

actual fun getPlatform(): Platform = MacosPlatform()

actual fun getCurrentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
