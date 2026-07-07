package com.techsultan.zenithpro.features.inventory.domain.use_case

import android.net.Uri
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.inventory.data.remote.UpdateProductRequest
import com.techsultan.zenithpro.features.inventory.domain.repository.ProductRepository

class UpdateProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(
        request: UpdateProductRequest,
        newImageUris: List<Uri> = emptyList()
    ): Resource<Unit> {
        if (request.name.isBlank()) return Resource.Error("Product name is required")
        if (request.baseSalesPrice < 0) return Resource.Error("Invalid sales price")
        if (request.baseCostPrice < 0) return Resource.Error("Invalid cost price")

        if (request.variants.isEmpty()) {
            return Resource.Error("At least one variant is required")
        }

        return repository.updateProduct(request, newImageUris)
    }
}