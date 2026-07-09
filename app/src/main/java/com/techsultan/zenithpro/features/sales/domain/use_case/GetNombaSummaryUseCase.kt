package com.techsultan.zenithpro.features.sales.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.sales.domain.model.NombaSummary
import com.techsultan.zenithpro.features.sales.domain.repository.NombaRepository
import kotlinx.coroutines.flow.Flow

class GetNombaSummaryUseCase(
    private val repository: NombaRepository
) {
    operator fun invoke(businessId: String): Flow<Resource<NombaSummary>> {
        return repository.getNombaSummary(businessId)
    }
}
