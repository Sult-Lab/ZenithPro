package com.techsultan.zenithpro.features.sales.domain.repository

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.sales.domain.model.NombaSummary
import com.techsultan.zenithpro.features.sales.domain.model.NombaTransfer
import kotlinx.coroutines.flow.Flow

interface NombaRepository {
    fun getNombaTransfers(businessId: String): Flow<Resource<List<NombaTransfer>>>
    fun getNombaSummary(businessId: String): Flow<Resource<NombaSummary>>
    suspend fun confirmTransfer(transferId: String): Resource<Unit>
    suspend fun refreshTransfers(businessId: String): Resource<Unit>
}
