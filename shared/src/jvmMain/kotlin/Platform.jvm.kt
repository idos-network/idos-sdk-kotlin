package org.idos

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

class JvmSecureRandom : SecureRandom {
    private val secureRandom get() = java.security.SecureRandom()

    override fun nextInt(int: Int): Int = secureRandom.nextInt(int)
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

actual fun getSecureRandom(): SecureRandom = JvmSecureRandom()
