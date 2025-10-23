package org.idos.enclave.crypto

import org.idos.getSecureRandom

/**
 * Shamir Secret Sharing over GF(2^8).
 *
 * Implements proper Shamir Secret Sharing using Galois Field arithmetic.
 * This matches the implementation in idos-sdk-js for compatibility.
 *
 * Key concepts:
 * - Secret is split byte-by-byte
 * - Each byte becomes the constant term of a polynomial over GF(2^8)
 * - Polynomial degree = k-1 (threshold - 1)
 * - Each share is an evaluation of the polynomial at x = 1, 2, 3, ..., n
 * - Reconstruction uses Lagrange interpolation in GF(2^8)
 *
 * Reference: idos-sdk-js/packages/utils/src/mpc/secretsharing/
 */
object ShamirSharing {
    /**
     * Split a secret into n shares using Shamir Secret Sharing over GF(2^8).
     *
     * The secret is split byte-by-byte. For each byte:
     * 1. Create a polynomial of degree (k-1) with the byte as constant term
     * 2. Generate (k-1) random coefficients for higher-degree terms
     * 3. Evaluate polynomial at x = 1, 2, ..., n to get n shares
     *
     * @param secret The secret to split
     * @param n Total number of shares to create
     * @param k Threshold - minimum number of shares needed to reconstruct
     * @return List of n shares, each the same length as the secret
     */
    fun splitByteWiseShamir(
        secret: ByteArray,
        n: Int,
        k: Int,
    ): List<ByteArray> {
        require(k <= n) { "Threshold k must be <= total shares n" }
        require(k > 0) { "Threshold k must be > 0" }
        require(n > 0) { "Number of shares n must be > 0" }

        // Initialize n shares, each will be the same length as the secret
        val shares = Array(n) { ByteArray(secret.size) }
        val random = getSecureRandom()

        // Get the x-coordinates (alphas) for evaluation: [1, 2, 3, ..., n]
        val alphas = GF256.alphas(n)

        // Process each byte of the secret
        secret.forEachIndexed { byteIndex, secretByte ->
            // Create polynomial: f(x) = secretByte + c1*x + c2*x^2 + ... + c(k-1)*x^(k-1)
            val coefficients = mutableListOf<GF256>()

            // Constant term is the secret byte
            coefficients.add(GF256.fromByte(secretByte))

            // Generate (k-1) random coefficients for higher-degree terms
            for (i in 1 until k) {
                coefficients.add(GF256.fromInt(random.nextInt(256)))
            }

            // Create the polynomial
            val polynomial = GF256Polynomial.fromCoefficients(coefficients)

            // Evaluate polynomial at each alpha to create shares
            for (shareIndex in 0 until n) {
                val shareValue = polynomial.evaluate(alphas[shareIndex])
                shares[shareIndex][byteIndex] = shareValue.toByte()
            }
        }

        return shares.toList()
    }

    /**
     * Reconstruct the secret from k or more shares using Lagrange interpolation.
     *
     * For each byte position:
     * 1. Extract the byte values from each share
     * 2. Create points (x, y) where x = 1, 2, 3, ... and y = share byte value
     * 3. Use Lagrange interpolation to reconstruct the polynomial
     * 4. The constant term of the polynomial is the original secret byte
     *
     * Important: Shares are assumed to come from consecutive indices starting at 0.
     * If you need to use non-consecutive shares, use combineByteWiseShamirWithIndices instead.
     *
     * @param shares The shares to reconstruct from (must have at least k shares)
     * @param k The threshold used when creating the shares
     * @return The reconstructed secret
     */
    fun combineByteWiseShamir(
        shares: List<ByteArray>,
        k: Int,
    ): ByteArray {
        // Assume shares come from indices 0, 1, 2, ...
        val indices = shares.indices.toList()
        return combineByteWiseShamirWithIndices(shares, indices, k)
    }

    /**
     * Reconstruct the secret from k or more shares using Lagrange interpolation,
     * with explicit share indices.
     *
     * Use this when you have non-consecutive shares (e.g., shares 0, 2, 4).
     *
     * @param shares The shares to reconstruct from
     * @param shareIndices The indices of these shares (0-based, corresponding to alphas 1, 2, 3, ...)
     * @param k The threshold used when creating the shares
     * @return The reconstructed secret
     */
    fun combineByteWiseShamirWithIndices(
        shares: List<ByteArray>,
        shareIndices: List<Int>,
        k: Int,
    ): ByteArray {
        require(shares.size >= k) { "Insufficient shares: have ${shares.size}, need $k" }
        require(shares.isNotEmpty()) { "Must provide at least one share" }
        require(shares.size == shareIndices.size) { "Must have same number of shares and indices" }

        val secretLength = shares[0].size
        val secret = ByteArray(secretLength)

        // Get the x-coordinates (alphas) for the specific share indices
        // shareIndex 0 corresponds to alpha 1, shareIndex 1 to alpha 2, etc.
        val alphas = shareIndices.map { GF256.fromInt(it + 1) }

        // Reconstruct each byte
        for (byteIndex in 0 until secretLength) {
            // Extract the y-values (share bytes) for this position
            val yValues =
                shares.map { share ->
                    GF256.fromByte(share[byteIndex])
                }

            // Create points (alpha, yValue) for Lagrange interpolation
            val points = alphas.zip(yValues)

            // Perform Lagrange interpolation to get f(0) = constant term
            val secretByte = lagrangeInterpolateAtZero(points)
            secret[byteIndex] = secretByte.toByte()
        }

        return secret
    }

    /**
     * Lagrange interpolation in GF(2^8) to find f(0).
     *
     * Given points (x1, y1), (x2, y2), ..., (xk, yk),
     * find the polynomial f(x) such that f(xi) = yi,
     * and return f(0) (the constant term).
     *
     * Lagrange formula:
     * f(x) = Σ yi * Li(x)
     * where Li(x) = Π (x - xj) / (xi - xj) for j ≠ i
     *
     * At x = 0:
     * f(0) = Σ yi * Li(0)
     * where Li(0) = Π (-xj) / (xi - xj) for j ≠ i
     *
     * @param points List of (x, y) points in GF(2^8)
     * @return f(0), the constant term of the interpolated polynomial
     */
    private fun lagrangeInterpolateAtZero(points: List<Pair<GF256, GF256>>): GF256 {
        var result = GF256.ZERO

        for (i in points.indices) {
            val (xi, yi) = points[i]

            // Compute the Lagrange basis polynomial Li(0)
            var numerator = GF256.ONE
            var denominator = GF256.ONE

            for (j in points.indices) {
                if (i != j) {
                    val xj = points[j].first

                    // Numerator: multiply by (0 - xj) = -xj = xj (since negation is identity in GF(2^8))
                    numerator *= xj

                    // Denominator: multiply by (xi - xj)
                    denominator *= (xi - xj)
                }
            }

            // Li(0) = numerator / denominator
            val lagrangeBasis = numerator / denominator

            // Add yi * Li(0) to result
            result += yi * lagrangeBasis
        }

        return result
    }
}
