package org.idos.kwil.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Base interface for string-based value types.
 * Provides type-safe wrappers around strings with validation.
 */
interface StringValue {
    val value: String
}

/**
 * Base64-encoded string value class.
 * Validates base64 format on construction.
 */
@JvmInline
@Serializable(with = Base64StringSerializer::class)
value class Base64String(
    override val value: String,
) : StringValue {
    init {
        require(value.isEmpty() || Base64.decode(value).isNotEmpty()) { "Invalid base64 string" }
    }

    constructor(bytes: ByteArray) : this(Base64.encode(bytes))
}

/**
 * Hexadecimal string value class.
 * Validates hex format on construction.
 */
@JvmInline
@Serializable(with = HexStringSerializer::class)
value class HexString(
    override val value: String,
) : StringValue {
    val prefixedValue get() = "0x$value"

    init {
        require(value.isEmpty() || value.matches(Regex("^[0-9a-fA-F]+$"))) { "Invalid hex string" }
    }

    constructor(bytes: ByteArray) : this(bytes.toHexString())

    companion object {
        fun withoutPrefix(hexString: String): HexString = HexString(hexString.removePrefix("0x"))

        fun String.toHexString(): HexString = withoutPrefix(this)
    }
}

/**
 * UUID string value class.
 * Validates UUID format on construction.
 */
@JvmInline
@Serializable(with = UuidStringSerializer::class)
value class UuidString(
    override val value: String,
) : StringValue {
    init {
        require(
            isValidUuid(value),
        ) { "Invalid UUID string" }
    }

    companion object {
        private val UUID_REGEX =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$")

        fun isValidUuid(value: String): Boolean = UUID_REGEX.matches(value)

        @OptIn(ExperimentalUuidApi::class)
        fun generate(): UuidString = UuidString(Uuid.random().toString())
    }
}

/**
 * Generic serializer for string value types.
 */
open class GenericStringSerializer<T : StringValue>(
    private val ctor: (String) -> T,
    serialName: String,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: T,
    ) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): T = ctor(decoder.decodeString())
}

object Base64StringSerializer : GenericStringSerializer<Base64String>(::Base64String, "Base64String")

object HexStringSerializer : GenericStringSerializer<HexString>(::HexString, "HexString")

object UuidStringSerializer : GenericStringSerializer<UuidString>(::UuidString, "UuidString")
