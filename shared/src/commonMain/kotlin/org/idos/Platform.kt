package org.idos

interface Platform {
    val name: String
}

interface SecureRandom {
    fun nextInt(int: Int): Int
}

expect fun getPlatform(): Platform

expect fun getCurrentTimeMillis(): Long

expect fun getSecureRandom(): SecureRandom
