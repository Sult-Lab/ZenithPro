package com.techsultan.zenithpro.features.inventory.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VariantAttributeDto(
    @SerialName("variant_id")       val variantId: String,
    @SerialName("option_value_id")  val optionValueId: String,
    // joined fields from product_option_values and product_options
    @SerialName("option_name")      val optionName: String,
    @SerialName("option_value")     val optionValue: String
)

