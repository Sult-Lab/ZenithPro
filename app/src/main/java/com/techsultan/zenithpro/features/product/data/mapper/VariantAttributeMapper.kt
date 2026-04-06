package com.techsultan.zenithpro.features.product.data.mapper

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.product.data.local.ProductEntity
import com.techsultan.zenithpro.features.product.data.local.ProductVariantEntity
import com.techsultan.zenithpro.features.product.data.local.VariantAttributeEntity
import com.techsultan.zenithpro.features.product.data.remote.ProductDto
import com.techsultan.zenithpro.features.product.data.remote.ProductVariantDto
import com.techsultan.zenithpro.features.product.data.remote.VariantAttributeDto

fun VariantAttributeDto.toEntity() = VariantAttributeEntity(
    id = "${variantId}_${optionValueId}",
    variantId = variantId,
    optionName = optionName,
    optionValue = optionValue
)