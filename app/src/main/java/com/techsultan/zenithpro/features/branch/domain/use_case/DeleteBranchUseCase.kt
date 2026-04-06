package com.techsultan.zenithpro.features.branch.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.branch.domain.repository.BranchRepository

class DeleteBranchUseCase(private val repository: BranchRepository) {
    suspend operator fun invoke(branchId: String) = repository.deleteBranch(branchId)
}