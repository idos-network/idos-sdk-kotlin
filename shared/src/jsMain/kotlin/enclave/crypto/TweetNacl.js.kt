package org.idos.enclave.crypto

import org.khronos.webgl.Uint8Array

/**
 * External declarations for tweetnacl library.
 * https://github.com/dchest/tweetnacl-js
 */
@JsModule("tweetnacl")
@JsNonModule
external object TweetNacl {
    /**
     * Generate random bytes.
     */
    fun randomBytes(length: Int): Uint8Array

    /**
     * Public-key authenticated encryption (box) namespace.
     */
    val box: TweetNaclBox
}

external interface TweetNaclBox {
    /**
     * Authenticates and decrypts the given box using peer's public key,
     * our secret key, and the given nonce.
     * Returns the original message, or null if authentication fails.
     */
    fun open(
        box: Uint8Array,
        nonce: Uint8Array,
        theirPublicKey: Uint8Array,
        mySecretKey: Uint8Array,
    ): Uint8Array?

    /**
     * Generate a new random key pair.
     */
    fun keyPair(): TweetNaclKeyPair

    /**
     * Length of public key in bytes (32).
     */
    val publicKeyLength: Int

    /**
     * Length of secret key in bytes (32).
     */
    val secretKeyLength: Int

    /**
     * Length of nonce in bytes (24).
     */
    val nonceLength: Int

    /**
     * Length of overhead (MAC) added to box in bytes (16).
     */
    val overheadLength: Int
}

external interface TweetNaclKeyPair {
    val publicKey: Uint8Array
    val secretKey: Uint8Array
}

/**
 * Encrypt using nacl.box()
 * nacl.box is a function, so we call it via dynamic.
 */
fun TweetNaclBox.encrypt(
    message: Uint8Array,
    nonce: Uint8Array,
    theirPublicKey: Uint8Array,
    mySecretKey: Uint8Array,
): Uint8Array? {
    return TweetNacl.asDynamic().box(message, nonce, theirPublicKey, mySecretKey).unsafeCast<Uint8Array?>()
}

/**
 * Create key pair from secret key.
 * nacl.box.keyPair.fromSecretKey(secretKey)
 */
fun TweetNaclBox.keyPairFromSecretKey(secretKey: Uint8Array): TweetNaclKeyPair {
    return this.asDynamic().keyPair.fromSecretKey(secretKey).unsafeCast<TweetNaclKeyPair>()
}
