package com.techsultan.zenithpro.core.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class BusinessDto(
    val id: String,
    val name: String,
    val phone: String? = null,
    val address: String? = null
)