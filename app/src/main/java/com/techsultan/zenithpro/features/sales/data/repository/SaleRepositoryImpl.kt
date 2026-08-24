package com.techsultan.zenithpro.features.sales.data.repository

import android.content.Context
import android.util.Log
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.branch.data.local.BranchDao
import com.techsultan.zenithpro.features.customer.data.local.DebtPaymentDao
import com.techsultan.zenithpro.features.customer.data.local.DebtPaymentEntity
import com.techsultan.zenithpro.features.customer.data.mapper.toEntity
import com.techsultan.zenithpro.features.customer.data.remote.DebtPaymentDto
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
import com.techsultan.zenithpro.features.sales.data.remote.SaleItemDto
import com.techsultan.zenithpro.features.sales.data.remote.SaleItemRequest
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
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class SaleRepositoryImpl(
    private val context: Context,
    private val saleDao: SaleDao,
    private val functions: Functions,
    private val postgrest: Postgrest,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager,
    private val debtPaymentDao: DebtPaymentDao,
    private val branchDao: BranchDao,
) : SaleRepository {

    override fun getSales(businessId: String) =
        saleDao.getSales(businessId)
            .map<List<SaleWithItems>, Resource<List<SaleWithItems>>> {
                Log.d("SaleRepo", "GetSales: returned ${it.size} items for businessId=$businessId")
                Resource.Success(it)
            }
            .catch { emit(Resource.Error(it.message ?: "Failed to load sales")) }
            .onStart {
                Log.d("SaleRepo", "getSales: starting for businessId=$businessId")
                emit(Resource.Loading())
            }

    override fun getSalesFiltered(businessId: String, filter: SaleFilter) =
        saleDao.getSalesFiltered(
            businessId    = businessId,
            from = filter.from,
            to  = filter.to,
            staffId = filter.staffId,
            paymentMethod = filter.paymentMethod,
            branchId = filter.branchId
        )
            .map<List<SaleWithItems>, Resource<List<SaleWithItems>>> {
                Log.d("SaleRepo", "getSalesFiltered: returned ${it.size} items for businessId=$businessId, filter=$filter")
                Resource.Success(it)
            }
            .catch { emit(Resource.Error(it.message ?: "Failed to load sales")) }
            .onStart {
                Log.d("SaleRepo", "getSalesFiltered: starting for businessId=$businessId, filter=$filter")
                emit(Resource.Loading())
            }

    override suspend fun processSale(
        request: ProcessSaleRequest,
        cart: List<CartItem>
    ): Resource<ProcessSaleResponse> = withContext(Dispatchers.IO) {
        try {
            // 1. Save sale locally first with PENDING status
            val saleId = UUID.randomUUID().toString()
            val now = Instant.now().toString()
            val businessId = sessionManager.businessId
            val debtAmount = maxOf(0L, request.totalAmount - request.amountPaid)
            val terminalId = sessionManager.currentSession?.terminalId

            if (request.branchId == null) {
                val branchCount = branchDao.getActiveBranchCount(businessId)
                if (branchCount > 0) {
                    return@withContext Resource.Error(
                        "A branch must be selected for this sale"
                    )
                }
            }
            saleDao.insertSale(
                SaleEntity(
                    id = saleId,
                    clientTransactionId = request.clientTransactionId,
                    businessId = businessId,
                    branchId = request.branchId,
                    customerId = request.customerId,
                    staffId = request.staffId,
                    subtotal = request.subtotal,
                    discountAmount = request.discountAmount,
                    taxAmount = request.taxAmount,
                    totalAmount = request.totalAmount,
                    amountPaid = request.amountPaid,
                    changeAmount = request.changeAmount,
                    debtAmount = debtAmount,
                    paymentMethod = PaymentMethod.valueOf(request.paymentMethod),
                    status = if (request.amountPaid >= request.totalAmount)
                        SaleStatus.COMPLETED else SaleStatus.PARTIAL,
                    notes = request.notes,
                    soldAt = now,
                    updatedAt = now,
                    syncStatus = Util.SyncStatus.PENDING,
                    terminalId = terminalId,
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
                        unitType = item.unitType,
                        unitPrice = item.unitPrice,
                        costPrice = item.costPrice,
                        quantity = item.quantity,
                        discount = item.discount,
                        totalPrice = item.totalPrice
                    )
                }
            )
            
            if (!networkMonitor.isConnected()) {
                return@withContext Resource.Success(
                    ProcessSaleResponse(
                        saleId = saleId,
                        status = "Saved locally. Sync pending.",
                        debtAmount = debtAmount,
                        idempotent = false,
                    )
                )
            }

            val remoteResult = pushSale(saleId)

            if (remoteResult != null) {
                Resource.Success(remoteResult)
            } else {
                Resource.Success(
                    ProcessSaleResponse(
                        saleId = saleId,
                        status = "Saved locally. Sync failed.",
                        debtAmount = debtAmount,
                        idempotent = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("SaleRepo", "processSale failed: ${e.message}", e)
            Resource.Error(e.message ?: "Sale failed")
        }
    }

    internal suspend fun pushSale(saleId: String): ProcessSaleResponse? {
        try {
            val saleWithItems = saleDao.getSaleById(saleId) ?: return null
            Log.d("SaleRepo", "pushSale: branchId=${saleWithItems.sale.branchId} saleId=$saleId")

            val request = ProcessSaleRequest(
                clientTransactionId = saleWithItems.sale.clientTransactionId,
                branchId = saleWithItems.sale.branchId,
                customerId = saleWithItems.sale.customerId,
                subtotal = saleWithItems.sale.subtotal,
                discountAmount = saleWithItems.sale.discountAmount,
                taxAmount = saleWithItems.sale.taxAmount,
                totalAmount = saleWithItems.sale.totalAmount,
                amountPaid = saleWithItems.sale.amountPaid,
                changeAmount = saleWithItems.sale.changeAmount,
                paymentMethod = saleWithItems.sale.paymentMethod.name,
                notes = saleWithItems.sale.notes,
                staffId = saleWithItems.sale.staffId,
                terminalId = saleWithItems.sale.terminalId,
                items = saleWithItems.items.map {
                    SaleItemRequest(
                        variantId = it.variantId,
                        productId = it.productId,
                        productName = it.productName,
                        variantSku = it.variantSku,
                        unitPrice = it.unitPrice,
                        costPrice = it.costPrice,
                        quantity = it.quantity,
                        discount = it.discount
                    )
                },
            )
            Log.d("SaleRepo", "pushSale: request=$request")
            val response = functions.invoke(
                function = "process_sale",
                body = request
            )
            
            val result = response.body<ProcessSaleResponse>()
            Log.d("SaleRepo", "pushSale: result=$result")
            saleDao.markSynced(saleId)
            Log.d("SaleRepo", "pushSale: Synced $saleId")
            return result
        } catch (e: Exception) {
            Log.e("SaleRepo", "pushSale failed for $saleId: ${e.message}")
            return null
        }
    }

    override suspend fun recordDebtPayment(
        request: DebtPaymentRequest
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val amountKobo = request.amount * 100
            val response = functions.invoke(
                function = "record_debt_payment",
                body     = request.copy(amount = amountKobo)
            )

            val result = response.body<DebtPaymentResponse>()

            // Save payment locally so it appears in the timeline immediately
            debtPaymentDao.insertPayment(
                DebtPaymentEntity(
                    id = result.paymentId,
                    businessId = sessionManager.businessId,
                    saleId = request.saleId,
                    customerId = request.customerId,
                    staffId = sessionManager.userId,
                    amount = amountKobo,
                    paymentMethod = request.paymentMethod,
                    notes = request.notes,
                    paidAt = Instant.now().toString(),
                    syncStatus = Util.SyncStatus.SYNCED
                )
            )

            val existingSale = saleDao.getSaleById(request.saleId)
            existingSale?.let { saleWithItems ->
                val sale = saleWithItems.sale
                val newAmountPaid = sale.amountPaid + amountKobo
                val newDebt = maxOf(0L, sale.totalAmount - newAmountPaid)
                val newStatus = if (newDebt <= 0L) SaleStatus.COMPLETED else SaleStatus.PARTIAL

                saleDao.insertSale(
                    sale.copy(
                        amountPaid  = newAmountPaid,
                        debtAmount  = newDebt,
                        status      = newStatus,
                        syncStatus  = Util.SyncStatus.SYNCED
                    )
                )
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("SaleRepo", "recordDebtPayment failed: ${e.message}", e)
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

            if (remoteSales.isEmpty()) return@withContext Resource.Success(Unit)

            val unsyncedIds = saleDao.getUnsyncedSales().map { it.clientTransactionId }.toSet()

            val toUpsert = remoteSales
                .filter { it.clientTransactionId !in unsyncedIds }
                .map { it.toEntity() }

            if (toUpsert.isNotEmpty()) {
                saleDao.insertSales(toUpsert)
            }

            val saleIds = remoteSales.map { it.id }

            val remoteItems = postgrest
                .from("sale_items")
                .select {
                    filter { isIn("sale_id", saleIds) }
                }
                .decodeList<SaleItemDto>()
            Log.d("SaleRepo", "pullSalesFromServer: ${remoteItems} items")

            postgrest
                .from("debt_payments")
                .select {
                    filter {
                        eq("business_id", businessId)
                        isIn("sale_id", saleIds)
                    }
                    order("paid_at", Order.DESCENDING)
                }
                .decodeList<DebtPaymentDto>()
                .map { it.toEntity() }
                .let { if (it.isNotEmpty()) debtPaymentDao.insertPayments(it) }

            Log.d("SaleRepo", "pullSalesFromServer: ${remoteSales.size} sales, ${remoteItems.size} items")

            if (remoteItems.isNotEmpty()) {
                saleDao.insertSaleItems(remoteItems.map { it.toEntity() })
            }

            val totalLocalSales = saleDao.getAllSales()
            Log.d("SaleRepo", "pullSalesFromServer: COMPLETED. Total sales in local DB: ${totalLocalSales.size}")
            totalLocalSales.forEach { 
                Log.d("SaleRepo", "Local Sale: id=${it.id}, businessId=${it.businessId}, branchId=${it.branchId}, soldAt=${it.soldAt}")
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("SaleRepo", "pullSalesFromServer error: ${e.message}", e)
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

    override suspend fun getNextSaleCounter(businessId: String): Int =
        saleDao.getNextSaleCounter(businessId)
}

@Serializable
data class DebtPaymentResponse(
    val paymentId: String,
    val newStatus: String,
    val remainingDebt: Long
)
