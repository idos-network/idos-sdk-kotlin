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

typealias HexString = String
typealias UuidString = String

object Uuid {
    private val UUID_REGEX =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$")

    @OptIn(ExperimentalUuidApi::class)
    fun generate(): UuidString =
        kotlin.uuid.Uuid
            .random()
            .toString()

    fun isValidUuid(value: String): Boolean = UUID_REGEX.matches(value)
}

fun UuidString.isValidUuid(): Boolean = Uuid.isValidUuid(this)

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
