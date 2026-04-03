package com.techsultan.zenithpro.features.customer.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CustomerRequest(
    val id: String?,
    val firstName: String,
    val lastName: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val notes: String?
)