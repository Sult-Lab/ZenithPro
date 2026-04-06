package com.techsultan.zenithpro.features.customer.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerDto(
    val id: String,
    @SerialName("business_id")  val businessId: String,
    @SerialName("first_name")   val firstName: String,
    @SerialName("last_name")    val lastName: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    @SerialName("total_spent")  val totalSpent: Long,
    @SerialName("total_debt")   val totalDebt: Long,
    @SerialName("visit_count")  val visitCount: Int,
    @SerialName("last_visit_at") val lastVisitAt: String?,
    val notes: String?,
    @SerialName("created_at")   val createdAt: String,
    @SerialName("updated_at")   val updatedAt: String,
    @SerialName("deleted_at")   val deletedAt: String? = null
)