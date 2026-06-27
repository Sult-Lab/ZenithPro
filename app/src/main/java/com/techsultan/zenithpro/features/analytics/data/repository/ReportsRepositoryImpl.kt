package com.techsultan.zenithpro.features.analytics.data.repository

import android.util.Log
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.analytics.data.ReportPeriod
import com.techsultan.zenithpro.features.analytics.data.remote.ReportsData
import com.techsultan.zenithpro.features.analytics.data.remote.StaffPerformance
import com.techsultan.zenithpro.features.analytics.domain.ReportsRepository
import com.techsultan.zenithpro.features.branch.data.local.BranchDao
import com.techsultan.zenithpro.features.customer.data.local.CustomerDao
import com.techsultan.zenithpro.features.customer.domain.repository.CustomerRepository
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseDao
import com.techsultan.zenithpro.features.expenses.domain.repository.ExpenseRepository
import com.techsultan.zenithpro.features.sales.data.local.SaleDao
import com.techsultan.zenithpro.features.sales.domain.repository.SaleRepository
import com.techsultan.zenithpro.features.settings.data.remote.StaffMember
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class ReportsRepositoryImpl(
    private val saleDao: SaleDao,
    private val expenseDao: ExpenseDao,
    private val customerDao: CustomerDao,
    private val branchDao: BranchDao,
    private val postgrest: Postgrest,
    private val saleRepository: SaleRepository,
    private val expenseRepository: ExpenseRepository,
    private val customerRepository: CustomerRepository,
    private val networkMonitor: NetworkMonitor,
) : ReportsRepository {

    override suspend fun getReportData(
        businessId: String,
        period: ReportPeriod,
        branchId: String?,
        staffId: String?
    ): Resource<ReportsData> = withContext(Dispatchers.IO) {
        try {
            val (from, to) = period.dateRange()
            val startOfFrom = LocalDate.parse(from)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
            val endOfTo = LocalDate.parse(to).plusDays(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toString()

            val salesSummaryJob = async {
                saleDao.getSummaryFiltered(businessId, startOfFrom, endOfTo, branchId, staffId)
            }
            val chartJob = async {
                saleDao.getChartDataFiltered(businessId, startOfFrom, endOfTo, branchId, staffId)
            }
            val expenseSummaryJob = async {
                expenseDao.getSummaryFiltered(businessId, from, to, branchId)
            }
            val expenseBreakdownJob = async {
                expenseDao.getCategoryBreakdownFiltered(businessId, from, to, branchId)
            }
            val topProductsJob = async {
                saleDao.getTopProducts(businessId, startOfFrom, endOfTo, branchId, staffId, 5)
            }
            val staffSalesJob = async {
                saleDao.getSalesByStaff(businessId, startOfFrom, endOfTo, branchId)
            }
            val customerStatsJob = async {
                customerDao.getCustomerStats(businessId)
            }
            val topSpendersJob = async {
                customerDao.getTopSpenders(businessId, 5)
            }
            val branchesJob = async {
                branchDao.getAllBranchIds(businessId).let {
                    if (it.isEmpty()) emptyList()
                    else branchDao.getBranches(businessId).first()
                }
            }

            val salesSummary    = salesSummaryJob.await()
            val expenseSummary  = expenseSummaryJob.await()

            val staffSalesRows = staffSalesJob.await()
            val staffNames = getAvailableStaffNames(
                businessId, staffSalesRows.map { it.staffId }
            )

            Resource.Success(
                ReportsData(
                    period           = period,
                    salesSummary     = salesSummary.copy(
                        profitMargin  = if (salesSummary.totalRevenue > 0)
                            (salesSummary.totalProfit.toFloat() / salesSummary.totalRevenue * 100)
                        else 0f
                    ),
                    expenseSummary = expenseSummary,
                    chartData = chartJob.await(),
                    expenseBreakdown = expenseBreakdownJob.await(),
                    topProducts = topProductsJob.await(),
                    staffPerformance = staffSalesRows.map {
                        StaffPerformance(
                            staffId = it.staffId,
                            staffName = staffNames[it.staffId] ?: "Unknown",
                            orderCount = it.orderCount,
                            revenue = it.revenue
                        )
                    },
                    customerStats = customerStatsJob.await(),
                    topSpenders = topSpendersJob.await(),
                    availableBranches = branchesJob.await()
                )
            )
        } catch (e: Exception) {
            Log.e("ReportsRepo", "getReportData error: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to load reports")
        }
    }

    override suspend fun getAvailableStaffNames(
        businessId: String,
        staffIds: List<String>
    ): Map<String, String> = withContext(Dispatchers.IO) {
        if (staffIds.isEmpty()) return@withContext emptyMap()
        try {
            postgrest.from("user_profiles")
                .select {
                    filter {
                        eq("business_id", businessId)
                        isIn("id", staffIds)
                    }
                }
                .decodeList<StaffMember>()
                .associate { it.id to it.fullName }
        } catch (e: Exception) {
            Log.w("ReportsRepo", "getAvailableStaffNames failed: ${e.message}")
            emptyMap()
        }
    }

    override suspend fun syncAll(businessId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            if (!networkMonitor.isConnected()) return@withContext Resource.Success(Unit)
            try {
                saleRepository.pullSalesFromServer(businessId)
                expenseRepository.pullFromServer(businessId)
                customerRepository.pullFromServer(businessId)
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Sync failed")
            }
        }
}