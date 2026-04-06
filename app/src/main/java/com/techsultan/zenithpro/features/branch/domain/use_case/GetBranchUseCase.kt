package com.techsultan.zenithpro.features.branch.domain.use_case

import com.techsultan.zenithpro.features.branch.domain.repository.BranchRepository

class GetBranchesUseCase(private val repository: BranchRepository) {
    operator fun invoke(businessId: String) = repository.getBranches(businessId)
}