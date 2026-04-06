package com.techsultan.zenithpro.features.material.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class RecipeWithMaterial(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "materialId", entityColumn = "id")
    val material: MaterialEntity
)