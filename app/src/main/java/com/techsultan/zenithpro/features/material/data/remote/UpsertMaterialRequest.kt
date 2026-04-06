package com.techsultan.zenithpro.features.material.data.remote

data class UpsertMaterialRequest(
    val id: String?,
    val name: String,
    val description: String?,
    val unit: String,
    val costPerUnit: Long,
    val lowStockAlert: Double?,
    val supplier: String?,
    val notes: String?
)