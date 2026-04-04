package com.techsultan.zenithpro.features.branch.domain.repository

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import kotlinx.coroutines.flow.Flow

interface BranchRepository {
    fun getBranches(businessId: String): Flow<Resource<List<BranchEntity>>>
    suspend fun upsertBranch(
        id: String?, name: String, address: String?,
        phone: String?, businessId: String
    ): Resource<BranchEntity>
    suspend fun toggleBranchActive(branchId: String, isActive: Boolean): Resource<Unit>
    suspend fun deleteBranch(branchId: String): Resource<Unit>
    suspend fun pullFromServer(businessId: String): Resource<Unit>
}