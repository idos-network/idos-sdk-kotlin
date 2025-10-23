package org.idos.crypto.eip712

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull
import org.idos.crypto.Keccak256Hasher

/**
 * EIP-712 typed structured data hashing and signing utilities.
 *
 * This implements the EIP-712 standard for hashing and signing typed structured data.
 * Reference: https://eips.ethereum.org/EIPS/eip-712
 *
 * All functions are static and require passing a Keccak256Hasher instance.
 */
object Eip712Utils {
    private const val EIP712_DOMAIN_TYPE = "EIP712Domain"

    /**
     * Compute the EIP-712 hash of typed data.
     * This is the final hash that should be signed.
     *
     * EIP-712 hash = keccak256Hash("\x19\x01" ‖ domainSeparator ‖ hashStruct(message))
     *
     * @param hasher The Keccak256 hasher implementation
     * @param typedData The typed data to hash
     * @return The 32-byte hash to sign
     */
    fun hashTypedData(
        hasher: Keccak256Hasher,
        typedData: TypedData,
    ): ByteArray {
        val domainSeparator = hashDomain(hasher, typedData.domain)
        val structHash = hashStruct(hasher, typedData.primaryType, typedData.types, typedData.message)

        // EIP-712 prefix: "\x19\x01"
        val prefix = byteArrayOf(0x19, 0x01)

        // Concatenate: prefix ‖ domainSeparator ‖ structHash
        val combined = prefix + domainSeparator + structHash

        return hasher.digest(combined)
    }

    /**
     * Hash the EIP-712 domain separator.
     *
     * @param hasher The Keccak256 hasher implementation
     * @param domain The domain parameters
     * @return The 32-byte domain separator hash
     */
    fun hashDomain(
        hasher: Keccak256Hasher,
        domain: TypedDataDomain,
    ): ByteArray {
        // Build domain type fields based on what's present
        val domainFields =
            mutableListOf(
                TypedDataField("name", "string"),
                TypedDataField("version", "string"),
            )

        domain.chainId?.let {
            domainFields.add(TypedDataField("chainId", "uint256"))
        }
        domainFields.add(TypedDataField("verifyingContract", "address"))

        val domainTypes = mapOf(EIP712_DOMAIN_TYPE to domainFields)

        // Build domain data
        val domainDataMap =
            mutableMapOf(
                "name" to JsonPrimitive(domain.name),
                "version" to JsonPrimitive(domain.version),
            )

        domain.chainId?.let {
            domainDataMap["chainId"] = JsonPrimitive(domain.chainId)
        }
        domainDataMap["verifyingContract"] = JsonPrimitive(domain.verifyingContract)

        val domainData = JsonObject(domainDataMap)

        return hashStruct(hasher, EIP712_DOMAIN_TYPE, domainTypes, domainData)
    }

    /**
     * Hash a struct according to EIP-712.
     *
     * hashStruct(s : 𝕊) = keccak256Hash(typeHash ‖ encodeData(s))
     *
     * @param hasher The Keccak256 hasher implementation
     * @param primaryType The primary type name
     * @param types All type definitions
     * @param data The struct data
     * @return The 32-byte struct hash
     */
    fun hashStruct(
        hasher: Keccak256Hasher,
        primaryType: String,
        types: Map<String, List<TypedDataField>>,
        data: JsonObject,
    ): ByteArray {
        val typeHashBytes = typeHash(hasher, primaryType, types)
        val encodedData = encodeData(hasher, primaryType, types, data)

        return hasher.digest(typeHashBytes + encodedData)
    }

    /**
     * Compute the type hash for a given type.
     *
     * typeHash(s) = keccak256Hash(encodeType(s))
     *
     * @param hasher The Keccak256 hasher implementation
     * @param primaryType The type name
     * @param types All type definitions
     * @return The 32-byte type hash
     */
    private fun typeHash(
        hasher: Keccak256Hasher,
        primaryType: String,
        types: Map<String, List<TypedDataField>>,
    ): ByteArray {
        val typeString = encodeType(primaryType, types)
        return hasher.digest(typeString.encodeToByteArray())
    }

    /**
     * Encode a type according to EIP-712.
     *
     * encodeType(s) = s ‖ "(" ‖ member₁ ‖ "," ‖ member₂ ‖ "," ‖ … ‖ memberₙ ")"
     *
     * @param primaryType The type name
     * @param types All type definitions
     * @return The encoded type string
     */
    fun encodeType(
        primaryType: String,
        types: Map<String, List<TypedDataField>>,
    ): String {
        val fields = types[primaryType] ?: throw IllegalArgumentException("Type $primaryType not found")

        // Primary type encoding
        val primary =
            buildString {
                append(primaryType)
                append("(")
                append(fields.joinToString(",") { "${it.type} ${it.name}" })
                append(")")
            }

        // Find all referenced custom types and sort them
        val referencedTypes = mutableSetOf<String>()
        findReferencedTypes(primaryType, types, referencedTypes)
        referencedTypes.remove(primaryType) // Don't include primary type

        val sortedRefs = referencedTypes.sorted()

        // Build full type string
        return buildString {
            append(primary)
            for (refType in sortedRefs) {
                val refFields = types[refType] ?: continue
                append(refType)
                append("(")
                append(refFields.joinToString(",") { "${it.type} ${it.name}" })
                append(")")
            }
        }
    }

    /**
     * Find all custom types referenced by a given type.
     */
    private fun findReferencedTypes(
        typeName: String,
        types: Map<String, List<TypedDataField>>,
        found: MutableSet<String>,
    ) {
        if (typeName in found) return
        if (typeName !in types) return // Not a custom type

        found.add(typeName)
        val fields = types[typeName] ?: return

        for (field in fields) {
            val baseType = field.type.removeSuffix("[]") // Handle arrays
            findReferencedTypes(baseType, types, found)
        }
    }

    /**
     * Encode the data for a struct according to EIP-712.
     *
     * encodeData(s : 𝕊) = enc(value₁) ‖ enc(value₂) ‖ … ‖ enc(valueₙ)
     *
     * @param hasher The Keccak256 hasher implementation
     * @param primaryType The type name
     * @param types All type definitions
     * @param data The struct data
     * @return The encoded data bytes
     */
    fun encodeData(
        hasher: Keccak256Hasher,
        primaryType: String,
        types: Map<String, List<TypedDataField>>,
        data: JsonObject,
    ): ByteArray {
        val fields = types[primaryType] ?: throw IllegalArgumentException("Type $primaryType not found")

        val encoded = mutableListOf<ByteArray>()

        for (field in fields) {
            val value = data[field.name] ?: JsonNull
            val fieldType = field.type

            val encodedVal = encodeValue(hasher, fieldType, value, types)
            encoded.add(encodedVal)
        }

        return encoded.fold(ByteArray(0)) { acc, bytes -> acc + bytes }
    }

    /**
     * Encode a single value according to EIP-712.
     */
    private fun encodeValue(
        hasher: Keccak256Hasher,
        type: String,
        value: JsonElement,
        types: Map<String, List<TypedDataField>>,
    ): ByteArray =
        when {
            value is JsonNull -> ByteArray(32) // null encodes as 32 zero bytes

            // Array types
            type.endsWith("[]") -> {
                val elementType = type.removeSuffix("[]")
                val array = value as? JsonArray ?: throw IllegalArgumentException("Expected array for $type")
                val encodedElements =
                    array.map { element ->
                        encodeValue(hasher, elementType, element, types)
                    }
                val concatenated = encodedElements.fold(ByteArray(0)) { acc, bytes -> acc + bytes }
                hasher.digest(concatenated)
            }

            // Custom struct types
            type in types -> {
                val structData = value as? JsonObject ?: throw IllegalArgumentException("Expected object for $type")
                hashStruct(hasher, type, types, structData)
            }

            // Bytes types
            type == "bytes" -> {
                val hexString = (value as JsonPrimitive).content
                val bytes = hexString.removePrefix("0x").hexToByteArray()
                hasher.digest(bytes)
            }

            type.startsWith("bytes") && type.length > 5 -> {
                // bytesN (fixed-length)
                val hexString = (value as JsonPrimitive).content
                val bytes = hexString.removePrefix("0x").hexToByteArray()
                padRight(bytes, 32)
            }

            // String type
            type == "string" -> {
                val bytes = (value as JsonPrimitive).content.encodeToByteArray()
                hasher.digest(bytes)
            }

            // Address type
            type == "address" -> {
                val addr = (value as JsonPrimitive).content.removePrefix("0x")
                val bytes = addr.hexToByteArray()
                padLeft(bytes, 32)
            }

            // Boolean type
            type == "bool" -> {
                val bool = (value as JsonPrimitive).booleanOrNull ?: false
                val byte = if (bool) 1.toByte() else 0.toByte()
                padLeft(byteArrayOf(byte), 32)
            }

            // Uint/int types
            type.startsWith("uint") || type.startsWith("int") -> {
                encodeInteger(value as JsonPrimitive)
            }

            else -> throw IllegalArgumentException("Unsupported type: $type")
        }

    /**
     * Encode an integer value.
     */
    private fun encodeInteger(value: JsonPrimitive): ByteArray {
        // Try to get as long directly from JSON primitive
        val number =
            value.longOrNull ?: run {
                val content = value.content
                when {
                    content.startsWith("0x") -> content.removePrefix("0x").toLong(16)
                    else -> content.toLongOrNull() ?: 0L
                }
            }

        // Convert to big-endian bytes (32 bytes for EIP-712)
        val bytes = ByteArray(32)
        for (i in 0 until 8) {
            bytes[31 - i] = (number shr (i * 8) and 0xFF).toByte()
        }

        return bytes
    }

    /**
     * Pad bytes on the left to reach the target length.
     */
    private fun padLeft(
        bytes: ByteArray,
        length: Int,
    ): ByteArray {
        if (bytes.size >= length) return bytes.copyOf(length)
        return ByteArray(length - bytes.size) + bytes
    }

    /**
     * Pad bytes on the right to reach the target length.
     */
    private fun padRight(
        bytes: ByteArray,
        length: Int,
    ): ByteArray {
        if (bytes.size >= length) return bytes.copyOf(length)
        return bytes + ByteArray(length - bytes.size)
    }
}
