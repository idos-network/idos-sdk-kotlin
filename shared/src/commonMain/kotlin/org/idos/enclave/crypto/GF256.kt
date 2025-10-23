package org.idos.enclave.crypto

/**
 * Represents an element in GF(2^8) - Galois Field with 256 elements.
 *
 * Uses the irreducible polynomial: x^8 + x^4 + x^3 + x + 1 (binary: 100011011 = 0x11B = 283)
 *
 * This is the proper mathematical field for Shamir Secret Sharing over bytes.
 * All arithmetic operations (add, subtract, multiply, divide) are performed in this field.
 *
 * Reference: Ported from idos-sdk-js/packages/utils/src/mpc/secretsharing/f256.ts
 */
class GF256 private constructor(
    val value: Int,
) {
    companion object {
        /**
         * The irreducible polynomial used for reduction: x^8 + x^4 + x^3 + x + 1
         * Binary: 0b100011011 = 283 = 0x11B
         */
        private const val REDUCTION_POLYNOMIAL = 0b100011011

        /**
         * Pre-computed multiplicative inverses for all non-zero elements.
         * inverse[i] gives the multiplicative inverse of i in GF(2^8).
         */
        private val MULTIPLICATIVE_INVERSE =
            intArrayOf(
                0x00,
                0x01,
                0x8d,
                0xf6,
                0xcb,
                0x52,
                0x7b,
                0xd1,
                0xe8,
                0x4f,
                0x29,
                0xc0,
                0xb0,
                0xe1,
                0xe5,
                0xc7,
                0x74,
                0xb4,
                0xaa,
                0x4b,
                0x99,
                0x2b,
                0x60,
                0x5f,
                0x58,
                0x3f,
                0xfd,
                0xcc,
                0xff,
                0x40,
                0xee,
                0xb2,
                0x3a,
                0x6e,
                0x5a,
                0xf1,
                0x55,
                0x4d,
                0xa8,
                0xc9,
                0xc1,
                0x0a,
                0x98,
                0x15,
                0x30,
                0x44,
                0xa2,
                0xc2,
                0x2c,
                0x45,
                0x92,
                0x6c,
                0xf3,
                0x39,
                0x66,
                0x42,
                0xf2,
                0x35,
                0x20,
                0x6f,
                0x77,
                0xbb,
                0x59,
                0x19,
                0x1d,
                0xfe,
                0x37,
                0x67,
                0x2d,
                0x31,
                0xf5,
                0x69,
                0xa7,
                0x64,
                0xab,
                0x13,
                0x54,
                0x25,
                0xe9,
                0x09,
                0xed,
                0x5c,
                0x05,
                0xca,
                0x4c,
                0x24,
                0x87,
                0xbf,
                0x18,
                0x3e,
                0x22,
                0xf0,
                0x51,
                0xec,
                0x61,
                0x17,
                0x16,
                0x5e,
                0xaf,
                0xd3,
                0x49,
                0xa6,
                0x36,
                0x43,
                0xf4,
                0x47,
                0x91,
                0xdf,
                0x33,
                0x93,
                0x21,
                0x3b,
                0x79,
                0xb7,
                0x97,
                0x85,
                0x10,
                0xb5,
                0xba,
                0x3c,
                0xb6,
                0x70,
                0xd0,
                0x06,
                0xa1,
                0xfa,
                0x81,
                0x82,
                0x83,
                0x7e,
                0x7f,
                0x80,
                0x96,
                0x73,
                0xbe,
                0x56,
                0x9b,
                0x9e,
                0x95,
                0xd9,
                0xf7,
                0x02,
                0xb9,
                0xa4,
                0xde,
                0x6a,
                0x32,
                0x6d,
                0xd8,
                0x8a,
                0x84,
                0x72,
                0x2a,
                0x14,
                0x9f,
                0x88,
                0xf9,
                0xdc,
                0x89,
                0x9a,
                0xfb,
                0x7c,
                0x2e,
                0xc3,
                0x8f,
                0xb8,
                0x65,
                0x48,
                0x26,
                0xc8,
                0x12,
                0x4a,
                0xce,
                0xe7,
                0xd2,
                0x62,
                0x0c,
                0xe0,
                0x1f,
                0xef,
                0x11,
                0x75,
                0x78,
                0x71,
                0xa5,
                0x8e,
                0x76,
                0x3d,
                0xbd,
                0xbc,
                0x86,
                0x57,
                0x0b,
                0x28,
                0x2f,
                0xa3,
                0xda,
                0xd4,
                0xe4,
                0x0f,
                0xa9,
                0x27,
                0x53,
                0x04,
                0x1b,
                0xfc,
                0xac,
                0xe6,
                0x7a,
                0x07,
                0xae,
                0x63,
                0xc5,
                0xdb,
                0xe2,
                0xea,
                0x94,
                0x8b,
                0xc4,
                0xd5,
                0x9d,
                0xf8,
                0x90,
                0x6b,
                0xb1,
                0x0d,
                0xd6,
                0xeb,
                0xc6,
                0x0e,
                0xcf,
                0xad,
                0x08,
                0x4e,
                0xd7,
                0xe3,
                0x5d,
                0x50,
                0x1e,
                0xb3,
                0x5b,
                0x23,
                0x38,
                0x34,
                0x68,
                0x46,
                0x03,
                0x8c,
                0xdd,
                0x9c,
                0x7d,
                0xa0,
                0xcd,
                0x1a,
                0x41,
                0x1c,
            )

        /**
         * Cache of all 256 field elements for reuse.
         */
        private val ELEMENTS = Array(256) { GF256(it) }

        /**
         * The zero element (additive identity).
         */
        val ZERO: GF256 = ELEMENTS[0]

        /**
         * The one element (multiplicative identity).
         */
        val ONE: GF256 = ELEMENTS[1]

        /**
         * Create a field element from a byte value.
         * @param value The byte value (0-255)
         * @return The corresponding GF(2^8) element
         */
        fun fromInt(value: Int): GF256 = ELEMENTS[value and 0xFF]

        /**
         * Create a field element from a byte.
         * @param value The byte value
         * @return The corresponding GF(2^8) element
         */
        fun fromByte(value: Byte): GF256 = ELEMENTS[value.toInt() and 0xFF]

        /**
         * Get the alphas (x-coordinates) for Shamir sharing with n nodes.
         * Returns [1, 2, 3, ..., n] as GF(2^8) elements.
         *
         * @param numNodes Number of nodes
         * @return Array of alpha values
         */
        fun alphas(numNodes: Int): List<GF256> = (1..numNodes).map { ELEMENTS[it] }
    }

    /**
     * Addition in GF(2^8) is XOR.
     * @param other The other element
     * @return this + other
     */
    operator fun plus(other: GF256): GF256 = ELEMENTS[value xor other.value]

    /**
     * Subtraction in GF(2^8) is the same as addition (XOR).
     * @param other The other element
     * @return this - other
     */
    operator fun minus(other: GF256): GF256 = ELEMENTS[value xor other.value]

    /**
     * Multiplication in GF(2^8) using the reduction polynomial.
     *
     * Implements polynomial multiplication with reduction modulo x^8 + x^4 + x^3 + x + 1.
     *
     * @param other The other element
     * @return this * other
     */
    operator fun times(other: GF256): GF256 {
        var a = this.value
        var b = other.value
        var product = 0

        // Perform polynomial multiplication with reduction
        for (i in 0 until 8) {
            // If the least significant bit of b is 1, XOR a into the product
            product = (product xor (-(b and 1) and a)) and 0xFF

            // Check if a will overflow (bit 7 is set)
            val mask = -((a shr 7) and 1) and 0xFF

            // Shift a left and reduce if necessary
            a = ((a shl 1) xor (REDUCTION_POLYNOMIAL and mask)) and 0xFF

            // Shift b right
            b = b shr 1
        }

        return ELEMENTS[product]
    }

    /**
     * Division in GF(2^8).
     * @param other The divisor (must not be zero)
     * @return this / other
     * @throws ArithmeticException if other is zero
     */
    operator fun div(other: GF256): GF256 {
        if (other.isZero()) {
            throw ArithmeticException("Division by zero in GF(2^8)")
        }
        return this * other.inverse()
    }

    /**
     * Multiplicative inverse in GF(2^8).
     * @return The multiplicative inverse of this element
     * @throws ArithmeticException if this element is zero
     */
    fun inverse(): GF256 {
        if (isZero()) {
            throw ArithmeticException("Cannot invert zero in GF(2^8)")
        }
        return ELEMENTS[MULTIPLICATIVE_INVERSE[value]]
    }

    /**
     * Negation in GF(2^8).
     * In characteristic 2 fields, -a = a.
     * @return -this (which equals this)
     */
    operator fun unaryMinus(): GF256 = this

    /**
     * Check if this element is zero.
     * @return true if this is the zero element
     */
    fun isZero(): Boolean = value == 0

    /**
     * Check if this element is one.
     * @return true if this is the one element
     */
    fun isOne(): Boolean = value == 1

    /**
     * Convert to byte.
     * @return The byte representation
     */
    fun toByte(): Byte = value.toByte()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GF256) return false
        return value == other.value
    }

    override fun hashCode(): Int = value

    override fun toString(): String = "GF256($value)"
}

/**
 * Polynomial over GF(2^8).
 *
 * Represents a polynomial with coefficients in GF(2^8).
 * Used for Shamir Secret Sharing.
 */
class GF256Polynomial(
    private val coefficients: List<GF256>,
) {
    init {
        require(coefficients.isNotEmpty()) { "Polynomial must have at least one coefficient" }
    }

    /**
     * Get the degree of this polynomial.
     * @return The degree (highest power of x with non-zero coefficient)
     */
    val degree: Int
        get() {
            for (i in coefficients.indices.reversed()) {
                if (!coefficients[i].isZero()) return i
            }
            return 0
        }

    /**
     * Get the constant term (coefficient of x^0).
     * @return The constant term
     */
    fun constantTerm(): GF256 = coefficients[0]

    /**
     * Evaluate the polynomial at a given point.
     *
     * Uses Horner's method for efficient evaluation:
     * p(x) = a0 + a1*x + a2*x^2 + ... = a0 + x*(a1 + x*(a2 + ...))
     *
     * @param x The point to evaluate at
     * @return p(x)
     */
    fun evaluate(x: GF256): GF256 {
        var result = GF256.ZERO
        // Horner's method: start from the highest degree coefficient
        for (i in coefficients.indices.reversed()) {
            result = result * x + coefficients[i]
        }
        return result
    }

    companion object {
        /**
         * Create a polynomial from coefficients.
         * @param coefficients The coefficients [a0, a1, a2, ...] for a0 + a1*x + a2*x^2 + ...
         * @return The polynomial
         */
        fun fromCoefficients(coefficients: List<GF256>): GF256Polynomial {
            require(coefficients.isNotEmpty()) { "Must have at least one coefficient" }
            return GF256Polynomial(coefficients)
        }
    }
}
