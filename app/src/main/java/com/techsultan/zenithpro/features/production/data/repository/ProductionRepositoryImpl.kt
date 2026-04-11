package com.techsultan.zenithpro.features.production.data.repository

import android.util.Log
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.production.data.local.ProductionOrderDao
import com.techsultan.zenithpro.features.production.data.local.ProductionOrderEntity
import com.techsultan.zenithpro.features.production.data.mapper.toEntity
import com.techsultan.zenithpro.features.production.data.remote.ProductionOrderDto
import com.techsultan.zenithpro.features.production.domain.repository.ProductionRepository
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

class ProductionRepositoryImpl(
    private val productionOrderDao: ProductionOrderDao,
    private val postgrest: Postgrest,
    private val functions: Functions,
    private val networkMonitor: NetworkMonitor,
) : ProductionRepository {

    override fun getOrders(businessId: String, status: String?) =
        productionOrderDao.getOrders(businessId, status)
            .map<List<ProductionOrderEntity>, Resource<List<ProductionOrderEntity>>> {
                Resource.Success(it)
            }
            .catch { emit(Resource.Error(it.message ?: "Failed")) }
            .onStart { emit(Resource.Loading()) }

    override suspend fun createOrder(
        variantId: String, quantity: Double, branchId: String?,
        notes: String?, businessId: String, staffId: String
    ): Resource<ProductionOrderEntity> = withContext(Dispatchers.IO) {
        try {
            val orderId = UUID.randomUUID().toString()
            val now = Instant.now().toString()
            val entity  = ProductionOrderEntity(
                id = orderId, businessId = businessId, branchId = branchId,
                variantId = variantId, quantity = quantity, status = "DRAFT",
                notes = notes, startedAt = null, completedAt = null,
                createdBy = staffId, createdAt = now, updatedAt = now,
                syncStatus = Util.SyncStatus.PENDING
            )
            productionOrderDao.insertOrder(entity)

            if (networkMonitor.isConnected()) {
                postgrest.from("production_orders").insert(
                    mapOf(
                        "id"          to orderId,
                        "business_id" to businessId,
                        "branch_id"   to branchId,
                        "variant_id"  to variantId,
                        "quantity"    to quantity,
                        "status"      to "DRAFT",
                        "notes"       to notes,
                        "created_by"  to staffId
                    )
                )
                productionOrderDao.markSynced(orderId, now)
            }
            Resource.Success(entity)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create order")
        }
    }

    override suspend fun startOrder(orderId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val now = Instant.now().toString()
                productionOrderDao.updateStatus(orderId, "IN_PROGRESS", now)
                if (networkMonitor.isConnected()) {
                    postgrest.from("production_orders")
                        .update(mapOf("status" to "IN_PROGRESS", "started_at" to now)) {
                            filter { eq("id", orderId) }
                        }
                }
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed")
            }
        }

    override suspend fun completeOrder(
        orderId: String, businessId: String, staffId: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!networkMonitor.isConnected()) {
                return@withContext Resource.Error(
                    "Internet required to complete production — materials must be deducted server-side"
                )
            }
            val response = functions.invoke(
                function = "complete-production-order",
                body     = mapOf(
                    "orderId"    to orderId,
                    "businessId" to businessId,
                    "staffId"    to staffId
                )
            )
            val now = Instant.now().toString()
            productionOrderDao.updateStatus(orderId, "COMPLETED", now)
            productionOrderDao.markSynced(orderId, now)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to complete order")
        }
    }

    override suspend fun cancelOrder(orderId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val now = Instant.now().toString()
                productionOrderDao.updateStatus(orderId, "CANCELLED", now)
                if (networkMonitor.isConnected()) {
                    postgrest.from("production_orders")
                        .update(mapOf("status" to "CANCELLED")) {
                            filter { eq("id", orderId) }
                        }
                }
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed")
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
                Resource.Error(e.message ?: "Pull failed")
            }
        }
}