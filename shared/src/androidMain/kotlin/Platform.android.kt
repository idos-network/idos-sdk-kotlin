package org.idos

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

class AndroidSecureRandom : SecureRandom {
    private val secureRandom = java.security.SecureRandom()

    override fun nextInt(int: Int): Int = secureRandom.nextInt(int)
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

actual fun getSecureRandom(): SecureRandom = AndroidSecureRandom()
