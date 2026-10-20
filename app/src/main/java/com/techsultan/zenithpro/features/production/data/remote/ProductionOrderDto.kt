package com.techsultan.zenithpro.features.production.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductionOrderDto(
    val id: String,
    @SerialName("business_id") val businessId: String,
    @SerialName("branch_id") val branchId: String?,
    @SerialName("variant_id") val variantId: String,
    val quantity: Double,
    val status: String,
    val notes: String?,
    @SerialName("started_at") val startedAt: String?,
    @SerialName("completed_at") val completedAt: String?,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val outcome: String? = "STOCK",
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("sale_id") val saleId: String? = null,
    @SerialName("actual_quantity") val actualQuantity: Double? = null,
    @SerialName("actual_material_cost") val actualMaterialCost: Long? = null,
    val remarks: String? = null,
    @SerialName("materials_deducted") val materialsDeducted: Boolean? = false,
    @SerialName("cancelled_at") val cancelledAt: String? = null
)

@Serializable
data class StartProductionOrderItem(
    @SerialName("materialId") val materialId: String,
    @SerialName("quantity") val quantity: Double
)

@Serializable
data class StartProductionOrderRequest(
    @SerialName("p_order_id") val orderId: String,
    @SerialName("p_items") val items: List<StartProductionOrderItem>
)

@Serializable
data class CompleteProductionOrderRequest(
    @SerialName("p_order_id") val orderId: String,
    @SerialName("p_branch_id") val branchId: String,
    @SerialName("p_actual_quantity") val actualQuantity: Double,
    @SerialName("p_completed_by") val completedBy: String,
    @SerialName("p_completed_by_name") val completedByName: String,
    @SerialName("p_completed_by_role") val completedByRole: String,
    @SerialName("p_customer_id") val customerId: String? = null,
    @SerialName("p_terminal_id") val terminalId: String? = null,
    @SerialName("p_staff_id") val staffId: String,
    @SerialName("p_unit_price") val unitPrice: Long? = null,
    @SerialName("p_amount_paid") val amountPaid: Long? = null,
    @SerialName("p_payment_method") val paymentMethod: String? = null,
    @SerialName("p_remarks") val remarks: String? = null
)

@Serializable
data class CancelProductionOrderRequest(
    @SerialName("p_order_id") val orderId: String
)
