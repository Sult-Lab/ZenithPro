package com.techsultan.zenithpro.features.inventory.domain.use_case

import android.net.Uri
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.inventory.data.remote.AddProductRequest
import com.techsultan.zenithpro.features.inventory.domain.repository.ProductRepository

class AddProductUseCase(private val productRepository: ProductRepository) {

    suspend operator fun invoke(
        addProductRequest: AddProductRequest,
        imageUris: List<Uri>
    ) : Resource<Unit> {
        return productRepository.addProduct(addProductRequest, imageUris)
    }

}