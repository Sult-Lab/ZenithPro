package com.techsultan.zenithpro.features.analytics.domain

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.analytics.data.ReportPeriod
import com.techsultan.zenithpro.features.analytics.data.remote.ReportsData

interface ReportsRepository {
    suspend fun getReportData(
        businessId: String,
        period: ReportPeriod,
        branchId: String?,
        staffId: String?
    ): Resource<ReportsData>

    suspend fun getAvailableStaffNames(
        businessId: String,
        staffIds: List<String>
    ): Map<String, String>

    suspend fun syncAll(businessId: String): Resource<Unit>
}