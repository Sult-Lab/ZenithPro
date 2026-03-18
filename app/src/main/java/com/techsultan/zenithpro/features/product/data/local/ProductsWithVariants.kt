package com.techsultan.zenithpro.features.product.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class ProductWithVariants(
    @Embedded val product: ProductEntity,
    @Relation(
        entity = ProductVariantEntity::class,
        parentColumn = "id",
        entityColumn = "productId"
    )
    val variants: List<VariantWithStock>
)

data class VariantWithStock(
    @Embedded val variant: ProductVariantEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "variantId"
    )
    val stock: List<ProductStockEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "variantId"
    )
    val attributes: List<VariantAttributeEntity>
)