package com.techsultan.zenithpro.features.settings.domain.repository

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.settings.data.local.BusinessSettingsEntity
import com.techsultan.zenithpro.features.settings.data.remote.CreateStaffRequest
import com.techsultan.zenithpro.features.settings.data.remote.CreateStaffResponse
import com.techsultan.zenithpro.features.settings.data.remote.StaffMember
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(businessId: String): Flow<BusinessSettingsEntity?>
    suspend fun getSettings(businessId: String): BusinessSettingsEntity?
    suspend fun updateSettings(settings: BusinessSettingsEntity): Resource<Unit>
    suspend fun updateUserRole(userId: String, newRole: String, businessId: String): Resource<Unit>
    suspend fun updateUserStatus(userId: String, status: String): Resource<Unit>
    suspend fun getStaffList(businessId: String): Resource<List<StaffMember>>
    suspend fun pullFromServer(businessId: String): Resource<Unit>
    suspend fun createStaff(request: CreateStaffRequest): Resource<CreateStaffResponse>
    suspend fun updateStaffBranch(userId: String, branchId: String?): Resource<Unit>
    suspend fun removeStaff(userId: String): Resource<Unit>
}