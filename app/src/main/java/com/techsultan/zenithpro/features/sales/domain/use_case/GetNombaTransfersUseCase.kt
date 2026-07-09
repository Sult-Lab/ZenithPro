package com.techsultan.zenithpro.features.sales.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.sales.domain.model.NombaTransfer
import com.techsultan.zenithpro.features.sales.domain.repository.NombaRepository
import kotlinx.coroutines.flow.Flow

class GetNombaTransfersUseCase(
    private val repository: NombaRepository
) {
    operator fun invoke(businessId: String): Flow<Resource<List<NombaTransfer>>> {
        return repository.getNombaTransfers(businessId)
    }
}
