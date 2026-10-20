package com.techsultan.zenithpro.features.dashboard.data.repository

import com.techsultan.zenithpro.core.util.ErrorSanitizer
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.branch.domain.repository.BranchRepository
import com.techsultan.zenithpro.features.dashboard.data.remote.ChartDataPoint
import com.techsultan.zenithpro.features.dashboard.data.remote.DashboardFulfillmentSummary
import com.techsultan.zenithpro.features.dashboard.data.remote.DashboardSummary
import com.techsultan.zenithpro.features.dashboard.data.remote.PendingDebtSummary
import com.techsultan.zenithpro.features.dashboard.domain.repository.DashboardRepository
import com.techsultan.zenithpro.features.inventory.domain.repository.ProductRepository
import com.techsultan.zenithpro.features.sales.data.local.SaleDao
import com.techsultan.zenithpro.features.sales.domain.repository.SaleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DashboardRepositoryImpl(
    private val saleDao: SaleDao,
    private val saleRepository: SaleRepository,
    private val productRepository: ProductRepository,
    private val branchRepository: BranchRepository,
) : DashboardRepository {

    override suspend fun getTodaySummary(
        businessId: String,
        branchId: String?
    ): Resource<DashboardSummary> = withContext(Dispatchers.IO) {
        try {
            val startOfDay = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toString()
            Resource.Success(saleDao.getTodaySummary(businessId, startOfDay, branchId))
        } catch (e: Exception) {
            Resource.Error(ErrorSanitizer.clean(e.message ?: "Failed to load summary"))
        }
    }

    override suspend fun getChartData(
        businessId: String,
        days: Int,
        branchId: String?
    ): Resource<List<ChartDataPoint>> = withContext(Dispatchers.IO) {
        try {
            val since = LocalDate.now()
                .minusDays(days.toLong() - 1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toString()
            val data = saleDao.getChartData(businessId, since, branchId)

            // Fill missing days with zero so the chart has no gaps
            val filled = fillMissingDays(data, days)
            Resource.Success(filled)
        } catch (e: Exception) {
            Resource.Error(ErrorSanitizer.clean(e.message ?: "Failed to load chart data"))
        }
    }

    override suspend fun getPendingDebts(
        businessId: String,
        branchId: String?
    ): Resource<PendingDebtSummary> = withContext(Dispatchers.IO) {
        try {
            Resource.Success(saleDao.getPendingDebts(businessId, branchId))
        } catch (e: Exception) {
            Resource.Error(ErrorSanitizer.clean(e.message ?: "Failed to load debts"))
        }
    }

    override suspend fun getWeeklyFulfillmentSummary(
        businessId: String,
        branchId: String?
    ): Resource<DashboardFulfillmentSummary> = withContext(Dispatchers.IO) {
        try {
            val startOfWeek = LocalDate.now()
                .with(DayOfWeek.MONDAY)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toString()
            val rows = saleDao.getWeeklyFulfillmentCounts(businessId, startOfWeek, branchId)
            val map = rows.associate { it.fulfillmentStatus to it.count }
            val pending = map["PENDING"] ?: 0
            // TODO: "processed" bucket maps to PROCESSING — unconfirmed with business owner if "processed" means PROCESSING or DELIVERED.
            val processing = map["PROCESSING"] ?: 0
            val delivered = map["DELIVERED"] ?: 0
            val failed = map["FAILED"] ?: 0
            val cancelled = map["CANCELLED"] ?: 0
            val total = pending + processing + delivered + failed + cancelled

            Resource.Success(
                DashboardFulfillmentSummary(
                    totalOrdersThisWeek = total,
                    pendingCount = pending,
                    processingCount = processing,
                    deliveredCount = delivered,
                    failedCount = failed,
                    cancelledCount = cancelled
                )
            )
        } catch (e: Exception) {
            Resource.Error(ErrorSanitizer.clean(e.message ?: "Failed to load fulfillment summary"))
        }
    }

    // Pull latest sales + products + branches so local Room reflects server state
    override suspend fun syncDashboard(businessId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                saleRepository.pullSalesFromServer(businessId)
                productRepository.pullFromServer(businessId)
                branchRepository.pullFromServer(businessId)
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(ErrorSanitizer.clean(e.message ?: "Sync failed"))
            }
        }

    // Ensure every day in the range has a data point — even if zero sales
    private fun fillMissingDays(
        data: List<ChartDataPoint>,
        days: Int
    ): List<ChartDataPoint> {
        val formatter  = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dataByDate = data.associateBy { it.saleDate }

        return (days - 1 downTo 0).map { daysAgo ->
            val date = LocalDate.now().minusDays(daysAgo.toLong())
                .format(formatter)
            dataByDate[date] ?: ChartDataPoint(
                saleDate = date,
                revenue = 0L,
                profit = 0L,
                orderCount = 0
            )
        }
    }
}
