package com.techsultan.zenithpro.features.settings.domain.use_case

import android.net.Uri
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util.trimOrNull
import com.techsultan.zenithpro.features.settings.data.remote.UpdateBusinessRequest
import com.techsultan.zenithpro.features.settings.data.remote.UpdateBusinessResponse
import com.techsultan.zenithpro.features.settings.domain.repository.BusinessRepository

class UpdateBusinessUseCase(private val repository: BusinessRepository) {
    suspend operator fun invoke(
        name: String,
        type: String?,
        phone: String?,
        email: String?,
        address: String?,
        currencyCode: String,
        currencySymbol: String,
        existingLogoUrl: String?,
        logoUri: Uri?
    ): Resource<UpdateBusinessResponse> {
        if (name.isBlank()) return Resource.Error("Business name is required")
        return repository.updateBusiness(
            request = UpdateBusinessRequest(
                name = name.trim(),
                type = type?.trimOrNull(),
                phone = phone?.trimOrNull(),
                email = email?.trimOrNull(),
                address = address?.trimOrNull(),
                logoUrl = existingLogoUrl,
                currencyCode = currencyCode,
                currencySymbol = currencySymbol
            ),
            logoUri = logoUri
        )
    }
}