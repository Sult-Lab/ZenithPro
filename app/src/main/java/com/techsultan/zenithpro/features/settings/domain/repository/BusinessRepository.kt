package com.techsultan.zenithpro.features.settings.domain.repository

import android.net.Uri
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.settings.data.remote.UpdateBusinessRequest
import com.techsultan.zenithpro.features.settings.data.remote.UpdateBusinessResponse

interface BusinessRepository {
    suspend fun updateBusiness(
        request: UpdateBusinessRequest,
        logoUri: Uri?
    ): Resource<UpdateBusinessResponse>
}