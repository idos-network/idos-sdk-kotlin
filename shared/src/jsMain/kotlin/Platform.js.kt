package org.idos

import kotlin.js.Date
import kotlin.random.Random

class JsPlatform : Platform {
    override val name: String = "JavaScript (Browser)"
}

class JsSecureRandom : SecureRandom {
    // Use Kotlin's Random which uses crypto.getRandomValues() in JS
    private val random = Random.Default

    override fun nextInt(int: Int): Int = random.nextInt(int)
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun getCurrentTimeMillis(): Long = Date.now().toLong()

actual fun getSecureRandom(): SecureRandom = JsSecureRandom()

