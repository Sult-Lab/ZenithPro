package com.techsultan.zenithpro.features.product.data.remote

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.product.data.local.ProductVariantEntity
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

