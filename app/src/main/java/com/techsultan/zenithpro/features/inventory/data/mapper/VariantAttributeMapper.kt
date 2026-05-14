package com.techsultan.zenithpro.features.inventory.data.mapper

import com.techsultan.zenithpro.features.inventory.data.local.VariantAttributeEntity
import com.techsultan.zenithpro.features.inventory.data.remote.VariantAttributeDto

fun VariantAttributeDto.toEntity() = VariantAttributeEntity(
    id = "${variantId}_${optionValueId}",
    variantId = variantId,
    optionName = optionName,
    optionValue = optionValue
)