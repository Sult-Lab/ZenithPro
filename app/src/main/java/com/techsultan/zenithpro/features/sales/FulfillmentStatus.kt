package com.techsultan.zenithpro.features.sales

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = FulfillmentStatusSerializer::class)
enum class FulfillmentStatus(val value: String) {
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    DELIVERED("DELIVERED"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED"),
    UNKNOWN("UNKNOWN");

    companion object {
        fun fromString(value: String?): FulfillmentStatus {
            if (value.isNull_or_blank()) return DELIVERED
            return entries.find {
                it.value.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
            } ?: UNKNOWN
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

object FulfillmentStatusSerializer : KSerializer<FulfillmentStatus> {
    override val descriptor = PrimitiveSerialDescriptor("FulfillmentStatus", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: FulfillmentStatus) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): FulfillmentStatus =
        FulfillmentStatus.fromString(decoder.decodeString())
}
