package com.techsultan.zenithpro.features.branch.data.repository

import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.branch.data.local.BranchDao
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.branch.data.mapper.toDto
import com.techsultan.zenithpro.features.branch.data.mapper.toEntity
import com.techsultan.zenithpro.features.branch.data.remote.BranchDto
import com.techsultan.zenithpro.features.branch.domain.repository.BranchRepository
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Objects.isNull
import java.util.UUID

class BranchRepositoryImpl(
    private val branchDao: BranchDao,
    private val postgrest: Postgrest,
    private val networkMonitor: NetworkMonitor,
) : BranchRepository {

    override fun getBranches(businessId: String) =
        branchDao.getBranches(businessId)
            .map<List<BranchEntity>, Resource<List<BranchEntity>>> { Resource.Success(it) }
            .catch { emit(Resource.Error(it.message ?: "Failed")) }
            .onStart { emit(Resource.Loading()) }

    override suspend fun upsertBranch(
        id: String?, name: String, address: String?,
        phone: String?, businessId: String
    ): Resource<BranchEntity> = withContext(Dispatchers.IO) {
        try {
            val branchId = id ?: UUID.randomUUID().toString()
            val now = Instant.now().toString()
            val existing = id?.let { branchDao.getBranchById(it) }

            val entity = BranchEntity(
                id = branchId, businessId = businessId,
                name = name, address = address, phone = phone,
                isActive = existing?.isActive ?: true,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now, deletedAt = null,
                syncStatus = Util.SyncStatus.PENDING
            )
            branchDao.insertBranch(entity)

            if (networkMonitor.isConnected()) {
                postgrest.from("branches").upsert(entity.toDto()) { onConflict = "id" }
                branchDao.markSynced(branchId, now)
            }
            Resource.Success(entity)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save branch")
        }
    }

    override suspend fun toggleBranchActive(
        branchId: String, isActive: Boolean
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = branchDao.getBranchById(branchId)
                ?: return@withContext Resource.Error("Branch not found")
            val updated = existing.copy(
                isActive   = isActive,
                updatedAt  = Instant.now().toString(),
                syncStatus = Util.SyncStatus.DIRTY
            )
            branchDao.insertBranch(updated)
            if (networkMonitor.isConnected()) {
                postgrest.from("branches")
                    .update(mapOf("is_active" to isActive)) {
                        filter { eq("id", branchId) }
                    }
                branchDao.markSynced(branchId, updated.updatedAt)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed")
        }
    }

    override suspend fun deleteBranch(branchId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val now = Instant.now().toString()
                branchDao.softDelete(branchId, now)
                if (networkMonitor.isConnected()) {
                    postgrest.from("branches")
                        .update(mapOf("deleted_at" to now)) {
                            filter { eq("id", branchId) }
                        }
                    branchDao.hardDelete(branchId)
                }
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to delete branch")
            }
        }

    override suspend fun pullFromServer(businessId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val remote = postgrest.from("branches").select {
                    filter { eq("business_id", businessId); isNull("deleted_at") }
                }.decodeList<BranchDto>()

                val unsyncedIds = branchDao.getUnsyncedBranches().map { it.id }.toSet()
                val toUpsert    = remote.filter { it.id !in unsyncedIds }.map { it.toEntity() }
                if (toUpsert.isNotEmpty()) branchDao.insertBranches(toUpsert)

                val remoteIds = remote.map { it.id }.toSet()
                branchDao.getAllBranchIds(businessId)
                    .filter { it !in remoteIds && it !in unsyncedIds }
                    .forEach { branchDao.hardDelete(it) }

                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Pull failed")
            }
        }
}