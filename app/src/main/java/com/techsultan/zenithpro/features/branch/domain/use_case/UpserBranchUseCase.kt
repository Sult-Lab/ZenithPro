package com.techsultan.zenithpro.features.branch.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.branch.domain.repository.BranchRepository

class UpsertBranchUseCase(private val repository: BranchRepository) {
    suspend operator fun invoke(
        id: String?, name: String, address: String?,
        phone: String?, businessId: String
    ): Resource<BranchEntity> {
        if (name.isBlank()) return Resource.Error("Branch name is required")
        return repository.upsertBranch(id, name, address, phone, businessId)
    }
}