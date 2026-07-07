package com.techsultan.zenithpro.features.branch.data.repository

import android.util.Log
import com.techsultan.zenithpro.core.manager.SessionManager
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
    private val sessionManager: SessionManager,
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
                pushBranch(entity)
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
                pushBranch(updated)
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
                    pushDelete(branchId)
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
                    filter {
                        eq("business_id", businessId)
                        isNull("deleted_at")
                    }
                }.decodeList<BranchDto>()

                val unsyncedIds = branchDao.getUnsyncedBranches().map { it.id }.toSet()
                val toUpsert    = remote.filter { it.id !in unsyncedIds }.map { it.toEntity() }
                if (toUpsert.isNotEmpty()) branchDao.insertBranches(toUpsert)

                val remoteIds = remote.map { it.id }.toSet()
                branchDao.getAllBranchIds(businessId)
                    .filter { it !in remoteIds && it !in unsyncedIds }
                    .forEach { branchDao.hardDelete(it) }

                // If session has no branch but only one branch exists,
                // update session so checkout auto-resolves without waiting
                val session = sessionManager.currentSession
                if (session != null &&
                    !session.isAdmin &&
                    session.branchId == null &&
                    remote.size == 1) {

                    val onlyBranch = remote.first()
                    sessionManager.saveSession(
                        session.copy(
                            branchId   = onlyBranch.id,
                            branchName = onlyBranch.name
                        )
                    )
                    Log.d("BranchRepo", "Auto-updated session branch to: ${onlyBranch.name}")
                }

                Resource.Success(Unit)
            } catch (e: Exception) {
                Log.e("BranchRepo", "pullFromServer error: ${e.message}", e)
                Resource.Error(e.message ?: "Pull failed")
            }
        }

    internal suspend fun pushBranch(entity: BranchEntity) {
        try {
            Log.d("BranchRepo", "pushBranch: ${entity.id}")
            postgrest.from("branches").upsert(entity.toDto()) { onConflict = "id" }
            branchDao.markSynced(entity.id, Instant.now().toString())
            Log.d("BranchRepo", "pushBranch: synced ${entity.id}")
        } catch (e: Exception) {
            Log.w("BranchRepo", "pushBranch failed for ${entity.id}: ${e.message}")
        }
    }

    internal suspend fun pushDelete(branchId: String) {
        try {
            Log.d("BranchRepo", "pushDelete: $branchId")
            postgrest.from("branches")
                .update(mapOf("deleted_at" to Instant.now().toString())) {
                    filter { eq("id", branchId) }
                }
            branchDao.hardDelete(branchId)
            Log.d("BranchRepo", "pushDelete: success for $branchId")
        } catch (e: Exception) {
            Log.w("BranchRepo", "pushDelete failed for $branchId: ${e.message}")
        }
    }
}
