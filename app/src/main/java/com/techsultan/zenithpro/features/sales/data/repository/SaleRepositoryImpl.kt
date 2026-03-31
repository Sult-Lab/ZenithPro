package com.techsultan.zenithpro.features.sales.data.repository

import android.util.Log
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.SaleStatus
import com.techsultan.zenithpro.features.sales.data.local.SaleDao
import com.techsultan.zenithpro.features.sales.data.local.SaleEntity
import com.techsultan.zenithpro.features.sales.data.local.SaleItemEntity
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import com.techsultan.zenithpro.features.sales.data.mapper.toEntity
import com.techsultan.zenithpro.features.sales.data.remote.CartItem
import com.techsultan.zenithpro.features.sales.data.remote.DailySummary
import com.techsultan.zenithpro.features.sales.data.remote.DebtPaymentRequest
import com.techsultan.zenithpro.features.sales.data.remote.ProcessSaleRequest
import com.techsultan.zenithpro.features.sales.data.remote.ProcessSaleResponse
import com.techsultan.zenithpro.features.sales.data.remote.SaleDto
import com.techsultan.zenithpro.features.sales.data.remote.SaleFilter
import com.techsultan.zenithpro.features.sales.domain.repository.SaleRepository
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class SaleRepositoryImpl(
    private val saleDao: SaleDao,
    private val functions: Functions,
    private val postgrest: Postgrest,
    private val networkMonitor: NetworkMonitor,
) : SaleRepository {

    override fun getSales(businessId: String) =
        saleDao.getSales(businessId)
            .map<List<SaleWithItems>, Resource<List<SaleWithItems>>> { Resource.Success(it) }
            .catch { emit(Resource.Error(it.message ?: "Failed to load sales")) }
            .onStart { emit(Resource.Loading()) }

    override fun getSalesFiltered(businessId: String, filter: SaleFilter) =
        saleDao.getSalesFiltered(
            businessId    = businessId,
            from          = filter.from,
            to            = filter.to,
            staffId       = filter.staffId,
            paymentMethod = filter.paymentMethod
        )
            .map<List<SaleWithItems>, Resource<List<SaleWithItems>>> { Resource.Success(it) }
            .catch { emit(Resource.Error(it.message ?: "Failed to load sales")) }
            .onStart { emit(Resource.Loading()) }

    override suspend fun processSale(
        request: ProcessSaleRequest,
        cart: List<CartItem>
    ): Resource<ProcessSaleResponse> = withContext(Dispatchers.IO) {
        try {
            // 1. Save sale locally first with PENDING status
            val saleId = UUID.randomUUID().toString()
            val now = Instant.now().toString()

            saleDao.insertSale(
                SaleEntity(
                    id = saleId,
                    clientTransactionId = request.clientTransactionId,
                    businessId = request.items.first().let { "" }, // set from session
                    branchId = request.branchId,
                    customerId = request.customerId,
                    staffId = "",  // set from session
                    subtotal = request.subtotal,
                    discountAmount = request.discountAmount,
                    taxAmount = request.taxAmount,
                    totalAmount = request.totalAmount,
                    amountPaid = request.amountPaid,
                    changeAmount = request.changeAmount,
                    debtAmount = maxOf(0L, request.totalAmount - request.amountPaid),
                    paymentMethod = PaymentMethod.valueOf(request.paymentMethod),
                    status = if (request.amountPaid >= request.totalAmount)
                        SaleStatus.COMPLETED else SaleStatus.PARTIAL,
                    notes = request.notes,
                    soldAt = now,
                    syncStatus = Util.SyncStatus.PENDING
                )
            )
            saleDao.insertSaleItems(
                cart.map { item ->
                    SaleItemEntity(
                        id = UUID.randomUUID().toString(),
                        saleId = saleId,
                        variantId = item.variantId,
                        productId = item.productId,
                        productName = item.productName,
                        variantSku = item.variantSku,
                        unitPrice = item.unitPrice,
                        costPrice = item.costPrice,
                        quantity = item.quantity,
                        discount = item.discount,
                        totalPrice = item.totalPrice
                    )
                }
            )

            // 2. Push to server — sales MUST sync immediately
            //    (inventory deduction happens server-side atomically)
            if (!networkMonitor.isConnected()) {
                return@withContext Resource.Error(
                    "No internet connection. Please connect and try again."
                )
            }

            val response = functions.invoke(
                function = "process_sale",
                body = request
            )
            val result = response.body<ProcessSaleResponse>()

            // 3. Update local record with server-confirmed ID and SYNCED status
            saleDao.markSynced(saleId)

            Resource.Success(result)
        } catch (e: Exception) {
            Log.e("SaleRepo", "processSale failed: ${e.message}", e)
            Resource.Error(e.message ?: "Sale failed")
        }
    }

    override suspend fun recordDebtPayment(
        request: DebtPaymentRequest
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            functions.invoke(function = "record-debt-payment", body = request)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Payment failed")
        }
    }

    override suspend fun pullSalesFromServer(
        businessId: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val remoteSales = postgrest
                .from("sales")
                .select {
                    filter { eq("business_id", businessId) }
                    order("sold_at", Order.DESCENDING)
                    limit(200)   // last 200 sales for local cache
                }
                .decodeList<SaleDto>()

            val unsyncedIds = saleDao.getUnsyncedSales().map { it.clientTransactionId }.toSet()

            val toUpsert = remoteSales
                .filter { it.clientTransactionId !in unsyncedIds }
                .map { it.toEntity() }

            if (toUpsert.isNotEmpty()) {
                saleDao.insertSales(toUpsert)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Pull failed")
        }
    }

    override suspend fun getDailySummary(businessId: String): Resource<DailySummary> =
        withContext(Dispatchers.IO) {
            try {
                val startOfDay = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toString()
                Resource.Success(saleDao.getDailySummary(businessId, startOfDay))
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to load summary")
            }
        }
}