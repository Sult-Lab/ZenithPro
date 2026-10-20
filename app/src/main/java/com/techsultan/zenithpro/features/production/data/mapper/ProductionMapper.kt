package com.techsultan.zenithpro.features.production.data.mapper

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.production.data.local.ProductionOrderEntity
import com.techsultan.zenithpro.features.production.data.remote.ProductionOrderDto
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

fun ProductionOrderDto.toEntity() = ProductionOrderEntity(
    id = id,
    businessId = businessId,
    branchId = branchId,
    variantId = variantId,
    quantity = quantity,
    status = status,
    notes = notes,
    startedAt = startedAt,
    completedAt = completedAt,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    outcome = outcome ?: "STOCK",
    customerId = customerId,
    saleId = saleId,
    actualQuantity = actualQuantity,
    actualMaterialCost = actualMaterialCost,
    remarks = remarks,
    materialsDeducted = materialsDeducted ?: false,
    cancelledAt = cancelledAt,
    syncStatus = Util.SyncStatus.SYNCED
)

fun ProductionOrderEntity.toDto() = ProductionOrderDto(
    id = id,
    businessId = businessId,
    branchId = branchId,
    variantId = variantId,
    quantity = quantity,
    status = status,
    notes = notes,
    startedAt = startedAt,
    completedAt = completedAt,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    outcome = outcome,
    customerId = customerId,
    saleId = saleId,
    actualQuantity = actualQuantity,
    actualMaterialCost = actualMaterialCost,
    remarks = remarks,
    materialsDeducted = materialsDeducted,
    cancelledAt = cancelledAt
)

@Serializable(with = ProductionStatusSerializer::class)
enum class ProductionStatus(val value: String) {
    DRAFT("DRAFT"),
    PENDING("PENDING"),
    IN_PROGRESS("IN_PROGRESS"),
    ON_HOLD("ON_HOLD"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED"),
    UNKNOWN("UNKNOWN")
}

object ProductionStatusSerializer : KSerializer<ProductionStatus> {
    override val descriptor = PrimitiveSerialDescriptor("ProductionStatus", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ProductionStatus) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): ProductionStatus =
        ProductionStatus.entries.find { it.value == decoder.decodeString() } ?: ProductionStatus.UNKNOWN
}
