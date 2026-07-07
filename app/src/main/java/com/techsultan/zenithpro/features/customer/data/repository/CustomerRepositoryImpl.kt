package com.techsultan.zenithpro.features.customer.data.repository

import android.util.Log
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.customer.data.local.CustomerDao
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.data.mapper.toEntity
import com.techsultan.zenithpro.features.customer.data.remote.CustomerDto
import com.techsultan.zenithpro.features.customer.data.remote.CustomerRequest
import com.techsultan.zenithpro.features.customer.data.remote.CustomerStats
import com.techsultan.zenithpro.features.customer.domain.repository.CustomerRepository
import com.techsultan.zenithpro.features.sales.data.local.SaleDao
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.Objects.isNull
import java.util.UUID

class CustomerRepositoryImpl(
    private val customerDao: CustomerDao,
    private val saleDao: SaleDao,
    private val functions: Functions,
    private val postgrest: Postgrest,
    private val networkMonitor: NetworkMonitor,
) : CustomerRepository {

    override fun getCustomers(businessId: String) =
        customerDao.getAllCustomers(businessId)
            .map<List<CustomerEntity>, Resource<List<CustomerEntity>>> { Resource.Success(it) }
            .catch { emit(Resource.Error(it.message ?: "Failed to load customers")) }
            .onStart { emit(Resource.Loading()) }

    override fun searchCustomers(businessId: String, query: String) =
        customerDao.searchCustomers(businessId, query)
            .map<List<CustomerEntity>, Resource<List<CustomerEntity>>> { Resource.Success(it) }
            .catch { emit(Resource.Error(it.message ?: "Search failed")) }

    override fun observeCustomer(customerId: String) =
        customerDao.observeCustomer(customerId)
            .map<CustomerEntity?, Resource<CustomerEntity?>> { Resource.Success(it) }
            .catch { emit(Resource.Error(it.message ?: "Failed to observe customer")) }

    override fun getCustomersWithDebt(businessId: String) =
        customerDao.getCustomersWithDebt(businessId)
            .map<List<CustomerEntity>, Resource<List<CustomerEntity>>> { Resource.Success(it) }
            .catch { emit(Resource.Error(it.message ?: "Failed to load debtors")) }


    override suspend fun upsertCustomer(
        request: CustomerRequest,
        businessId: String
    ): Resource<CustomerEntity> = withContext(Dispatchers.IO) {
        try {
            val clientId = request.id ?: UUID.randomUUID().toString()
            val now = Instant.now().toString()

            // Get existing entity if updating
            val existing = request.id?.let { customerDao.getCustomerById(it) }

            val entity = CustomerEntity(
                id          = clientId,
                businessId  = businessId,
                firstName   = request.firstName,
                lastName    = request.lastName,
                phone       = request.phone,
                email       = request.email,
                address     = request.address,
                notes       = request.notes,
                totalSpent  = existing?.totalSpent  ?: 0L,
                totalDebt   = existing?.totalDebt   ?: 0L,
                visitCount  = existing?.visitCount  ?: 0,
                lastVisitAt = existing?.lastVisitAt,
                createdAt   = existing?.createdAt   ?: now,
                updatedAt   = now,
                deletedAt   = null,
                syncStatus  = Util.SyncStatus.PENDING
            )

            // 1. Save locally first
            customerDao.insertCustomer(entity)

            // 2. Sync immediately if online
            if (networkMonitor.isConnected()) {
                pushCustomer(entity)
            }

            Resource.Success(entity)
        } catch (e: Exception) {
            Log.e("CustomerRepo", "upsertCustomer: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to save customer")
        }
    }


    override suspend fun deleteCustomer(customerId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val now = Instant.now().toString()
                customerDao.softDelete(customerId, now)
                if (networkMonitor.isConnected()) {
                    postgrest.from("customers")
                        .update(mapOf("deleted_at" to now)) {
                            filter { eq("id", customerId) }
                        }
                    customerDao.hardDelete(customerId)
                }
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to delete customer")
            }
        }


    override suspend fun getTopSpenders(
        businessId: String, limit: Int
    ): Resource<List<CustomerEntity>> = withContext(Dispatchers.IO) {
        try { Resource.Success(customerDao.getTopSpenders(businessId, limit)) }
        catch (e: Exception) { Resource.Error(e.message ?: "Failed") }
    }

    override suspend fun getMostLoyal(
        businessId: String, limit: Int
    ): Resource<List<CustomerEntity>> = withContext(Dispatchers.IO) {
        try { Resource.Success(customerDao.getMostLoyal(businessId, limit)) }
        catch (e: Exception) { Resource.Error(e.message ?: "Failed") }
    }

    override suspend fun getCustomerStats(
        businessId: String
    ): Resource<CustomerStats> = withContext(Dispatchers.IO) {
        try { Resource.Success(customerDao.getCustomerStats(businessId)) }
        catch (e: Exception) { Resource.Error(e.message ?: "Failed") }
    }


    override suspend fun getCustomerSales(
        customerId: String,
        businessId: String
    ): Resource<List<SaleWithItems>> = withContext(Dispatchers.IO) {
        try {
            // Sales are already local — just filter by customerId
            val sales = saleDao.getSalesByCustomer(customerId)
            Resource.Success(sales)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load transactions")
        }
    }

    override suspend fun pullFromServer(businessId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            if (businessId.isBlank()) {
                Log.e("CustomerRepo", "pullFromServer: businessId is blank")
                return@withContext Resource.Error("Business ID is missing")
            }
            try {
                val remote = postgrest
                    .from("customers")
                    .select {
                        filter {
                            eq("business_id", businessId)
                            isNull("deleted_at")
                        }
                    }
                    .decodeList<CustomerDto>()

                val unsyncedIds = customerDao.getUnsyncedCustomers()
                    .map { it.id }.toSet()

                val toUpsert = remote
                    .filter { it.id !in unsyncedIds }
                    .map { it.toEntity() }

                if (toUpsert.isNotEmpty()) {
                    customerDao.insertCustomers(toUpsert)
                }

                // Remove server-deleted customers locally
                val remoteIds  = remote.map { it.id }.toSet()
                val localIds   = customerDao.getAllCustomerIds(businessId).toSet()
                localIds
                    .filter { it !in remoteIds && it !in unsyncedIds }
                    .forEach { customerDao.hardDelete(it) }

                Resource.Success(Unit)
            } catch (e: Exception) {
                Log.e("CustomerRepo", "pullFromServer error: ${e.message}", e)
                Resource.Error(e.message ?: "Pull failed")
            }
        }


    internal suspend fun pushCustomer(entity: CustomerEntity) {
        try {
            val response = functions.invoke(
                function = "upsert_customer",
                body = mapOf(
                    "clientId"  to entity.id,
                    "firstName" to entity.firstName,
                    "lastName"  to entity.lastName,
                    "phone"     to entity.phone,
                    "email"     to entity.email,
                    "address"   to entity.address,
                    "notes"     to entity.notes
                )
            )

            @Serializable
            data class UpsertResponse(val customerId: String, val updatedAt: String)

            val result = response.body<UpsertResponse>()
            Log.d("CustomerRepo", "pushCustomer: $result")
            customerDao.markSynced(entity.id, result.updatedAt)
            Log.d("CustomerRepo", "pushCustomer: synced ${entity.id}")
        } catch (e: Exception) {
            Log.w("CustomerRepo", "pushCustomer failed for ${entity.id}: ${e.message}")
        }
    }
}
