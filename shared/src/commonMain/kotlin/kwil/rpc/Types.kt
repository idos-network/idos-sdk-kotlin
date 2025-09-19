package org.idos.kwil.rpc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(with = Base64StringSerializer::class)
value class Base64String(
    val value: String,
) {
    constructor(bytes: ByteArray) : this(Base64.encode(bytes))
}

@JvmInline
@Serializable(with = HexStringSerializer::class)
value class HexString(
    val value: String,
) {
    constructor(bytes: ByteArray) : this(bytes.toHexString())
}

object Base64StringSerializer : KSerializer<Base64String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Base64String", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Base64String,
    ) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): Base64String = Base64String(decoder.decodeString())
}

object HexStringSerializer : KSerializer<HexString> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HexString", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: HexString,
    ) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): HexString = HexString(decoder.decodeString())
}
