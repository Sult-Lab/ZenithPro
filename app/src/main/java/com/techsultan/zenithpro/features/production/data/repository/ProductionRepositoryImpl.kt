package com.techsultan.zenithpro.features.production.data.repository

import android.util.Log
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.ErrorSanitizer
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.material.data.local.MaterialDao
import com.techsultan.zenithpro.features.production.data.local.ProductionOrderDao
import com.techsultan.zenithpro.features.production.data.local.ProductionOrderEntity
import com.techsultan.zenithpro.features.production.data.mapper.toDto
import com.techsultan.zenithpro.features.production.data.mapper.toEntity
import com.techsultan.zenithpro.features.production.data.remote.CancelProductionOrderRequest
import com.techsultan.zenithpro.features.production.data.remote.CompleteProductionOrderRequest
import com.techsultan.zenithpro.features.production.data.remote.ProductionOrderDto
import com.techsultan.zenithpro.features.production.data.remote.StartProductionOrderItem
import com.techsultan.zenithpro.features.production.data.remote.StartProductionOrderRequest
import com.techsultan.zenithpro.features.production.domain.repository.ProductionRepository
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

class ProductionRepositoryImpl(
    private val productionOrderDao: ProductionOrderDao,
    private val materialDao: MaterialDao,
    private val postgrest: Postgrest,
    private val functions: Functions,
    private val networkMonitor: NetworkMonitor,
) : ProductionRepository {

    override fun getOrders(businessId: String, status: String?) =
        productionOrderDao.getOrders(businessId, status)
            .map<List<ProductionOrderEntity>, Resource<List<ProductionOrderEntity>>> {
                Resource.Success(it)
            }
            .catch { emit(Resource.Error(ErrorSanitizer.clean(it.message ?: "Failed"))) }
            .onStart { emit(Resource.Loading()) }

    override suspend fun createOrder(
        variantId: String, quantity: Double, branchId: String?,
        notes: String?, businessId: String, staffId: String
    ): Resource<ProductionOrderEntity> = withContext(Dispatchers.IO) {
        try {
            val orderId = UUID.randomUUID().toString()
            val now = Instant.now().toString()
            val entity = ProductionOrderEntity(
                id = orderId, businessId = businessId, branchId = branchId,
                variantId = variantId, quantity = quantity, status = "DRAFT",
                notes = notes, startedAt = null, completedAt = null,
                createdBy = staffId, createdAt = now, updatedAt = now,
                syncStatus = Util.SyncStatus.PENDING
            )
            productionOrderDao.insertOrder(entity)

            if (networkMonitor.isConnected()) {
                pushOrder(entity)
            }
            Resource.Success(entity)
        } catch (e: Exception) {
            Resource.Error(ErrorSanitizer.clean(e.message ?: "Failed to create order"))
        }
    }

    internal suspend fun pushOrder(entity: ProductionOrderEntity) {
        if (entity.status !in listOf("DRAFT", "PENDING")) {
            Log.w("ProductionRepo", "pushOrder skipped for ${entity.id} with status ${entity.status} — state transitions must use RPCs")
            return
        }
        try {
            postgrest.from("production_orders").upsert(entity.toDto()) { onConflict = "id" }
            productionOrderDao.markSynced(entity.id, Instant.now().toString())
            Log.d("ProductionRepo", "pushOrder: synced ${entity.id}")
        } catch (e: Exception) {
            Log.w("ProductionRepo", "pushOrder failed for ${entity.id}: ${e.message}")
        }
    }

    override suspend fun startOrder(
        orderId: String,
        overriddenItems: List<Pair<String, Double>>?
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!networkMonitor.isConnected()) {
                return@withContext Resource.Error(
                    ErrorSanitizer.clean("Internet required to start production — materials must be deducted server-side")
                )
            }
            val existing = productionOrderDao.getOrderById(orderId)
                ?: return@withContext Resource.Error(ErrorSanitizer.clean("Order not found"))

            val requestItems = if (!overriddenItems.isNullOrEmpty()) {
                overriddenItems.map { (matId, qty) ->
                    StartProductionOrderItem(materialId = matId, quantity = qty)
                }
            } else {
                val recipeItems = materialDao.getRecipesByVariant(existing.variantId)
                recipeItems.map { recipe ->
                    StartProductionOrderItem(
                        materialId = recipe.materialId,
                        quantity = recipe.quantityNeeded * existing.quantity
                    )
                }
            }

            val request = StartProductionOrderRequest(
                orderId = orderId,
                items = requestItems
            )

            postgrest.rpc(
                function = "start_production_order",
                parameters = request
            )
            Log.d("ProductionRepo", "startOrder: synced $orderId")
            val now = Instant.now().toString()
            productionOrderDao.updateStatusAndStartedAt(orderId, "IN_PROGRESS", now, now)
            productionOrderDao.markSynced(orderId, now)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("ProductionRepo", "startOrder error: ${e.message}", e)
            Resource.Error(ErrorSanitizer.clean(e.message ?: "Failed to start production order"))
        }
    }

    override suspend fun completeOrder(
        orderId: String,
        branchId: String,
        actualQuantity: Double,
        completedBy: String,
        completedByName: String,
        completedByRole: String,
        customerId: String?,
        terminalId: String?,
        unitPrice: Long?,
        amountPaid: Long?,
        paymentMethod: String?,
        remarks: String?
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!networkMonitor.isConnected()) {
                return@withContext Resource.Error(
                    ErrorSanitizer.clean("Internet required to complete production")
                )
            }
            val request = CompleteProductionOrderRequest(
                orderId = orderId,
                branchId = branchId,
                actualQuantity = actualQuantity,
                completedBy = completedBy,
                completedByName = completedByName,
                completedByRole = completedByRole,
                customerId = customerId?.takeIf { it.isNotBlank() },
                terminalId = terminalId?.takeIf { it.isNotBlank() },
                staffId = completedBy,
                unitPrice = unitPrice,
                amountPaid = amountPaid,
                paymentMethod = paymentMethod?.takeIf { it.isNotBlank() },
                remarks = remarks?.takeIf { it.isNotBlank() }
            )

            postgrest.rpc(
                function = "complete_production_order",
                parameters = request
            )

            val now = Instant.now().toString()
            productionOrderDao.updateCompletedOrder(
                id = orderId,
                actualQuantity = actualQuantity,
                remarks = remarks,
                completedAt = now,
                updatedAt = now
            )
            productionOrderDao.markSynced(orderId, now)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ErrorSanitizer.clean(e.message ?: "Failed to complete order"))
        }
    }

    override suspend fun cancelOrder(orderId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (!networkMonitor.isConnected()) {
                    return@withContext Resource.Error(
                        ErrorSanitizer.clean("Internet required to cancel production order")
                    )
                }
                val request = CancelProductionOrderRequest(orderId = orderId)
                postgrest.rpc(
                    function = "cancel_production_order",
                    parameters = request
                )

                val now = Instant.now().toString()
                productionOrderDao.updateStatusAndCancelledAt(orderId, "CANCELLED", now, now)
                productionOrderDao.markSynced(orderId, now)
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(ErrorSanitizer.clean(e.message ?: "Failed to cancel production order"))
            }
        }

    override suspend fun getPendingOrderCount(businessId: String): Int =
        productionOrderDao.getPendingOrderCount(businessId)

    override suspend fun pullFromServer(businessId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            if (businessId.isBlank()) {
                Log.e("ProductionRepo", "pullFromServer: businessId is blank")
                return@withContext Resource.Error("Business ID is missing")
            }
            try {
                val remote = postgrest.from("production_orders").select {
                    filter { eq("business_id", businessId) }
                    order("created_at", Order.DESCENDING)
                    limit(100)
                }.decodeList<ProductionOrderDto>()

                val unsyncedIds = productionOrderDao.getUnsyncedOrders().map { it.id }.toSet()
                remote.filter { it.id !in unsyncedIds }
                    .map { it.toEntity() }
                    .let { if (it.isNotEmpty()) productionOrderDao.insertOrders(it) }

                Resource.Success(Unit)
            } catch (e: Exception) {
                Log.e("ProductionRepo", "pullFromServer error: ${e.message}", e)
                Resource.Error(ErrorSanitizer.clean(e.message ?: "Pull failed"))
            }
        }
}
