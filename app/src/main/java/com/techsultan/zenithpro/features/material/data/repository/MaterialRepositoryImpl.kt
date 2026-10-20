package com.techsultan.zenithpro.features.material.data.repository

import android.util.Log
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.ErrorSanitizer
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.material.data.local.MaterialDao
import com.techsultan.zenithpro.features.material.data.local.MaterialEntity
import com.techsultan.zenithpro.features.material.data.local.RecipeEntity
import com.techsultan.zenithpro.features.material.data.local.RecipeWithMaterial
import com.techsultan.zenithpro.features.material.data.remote.RecipeItem
import com.techsultan.zenithpro.features.material.data.mapper.toDto
import com.techsultan.zenithpro.features.material.data.mapper.toEntity
import com.techsultan.zenithpro.features.material.data.remote.AdjustMaterialStockRequest
import com.techsultan.zenithpro.features.material.data.remote.MaterialDto
import com.techsultan.zenithpro.features.material.data.remote.RecipeDto
import com.techsultan.zenithpro.features.material.data.remote.RecipeInsertRequest
import com.techsultan.zenithpro.features.material.data.remote.RecipeUpdateRequest
import com.techsultan.zenithpro.features.material.data.remote.UpsertMaterialRequest
import com.techsultan.zenithpro.features.material.domain.repository.MaterialRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Objects.isNull
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class MaterialRepositoryImpl(
    private val materialDao: MaterialDao,
    private val postgrest: Postgrest,
    private val networkMonitor: NetworkMonitor,
) : MaterialRepository {

    override fun getMaterials(businessId: String, status: String?) =
        materialDao.getMaterials(businessId, status)
            .map<List<MaterialEntity>, Resource<List<MaterialEntity>>> { Resource.Success(it) }
            .catch { emit(Resource.Error(ErrorSanitizer.clean(it.message ?: "Failed"))) }
            .onStart { emit(Resource.Loading()) }

    override fun getLowStockMaterials(businessId: String) =
        materialDao.getLowStockMaterials(businessId)
            .map<List<MaterialEntity>, Resource<List<MaterialEntity>>> { Resource.Success(it) }
            .catch { emit(Resource.Error(ErrorSanitizer.clean(it.message ?: "Failed"))) }

    override fun getRecipeForVariant(variantId: String): Flow<Resource<List<RecipeWithMaterial>>> = flow {
        emit(Resource.Loading())
        coroutineScope {
            launch {
                if (networkMonitor.isConnected()) {
                    pullRecipeForVariant(variantId)
                }
            }
            emitAll(
                materialDao.getRecipeForVariant(variantId)
                    .map<List<RecipeWithMaterial>, Resource<List<RecipeWithMaterial>>> { Resource.Success(it) }
                    .catch { emit(Resource.Error(ErrorSanitizer.clean(it.message ?: "Failed"))) }
            )
        }
    }
    override fun getAllRecipes(
        businessId: String
    ): Flow<Resource<List<RecipeWithMaterial>>> = flow {
        coroutineScope {
            launch {
                if (networkMonitor.isConnected()) {
                    try {
                        pullAllRecipes(businessId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(
                            "MaterialRepositoryImpl",
                            "Recipe refresh failed for business $businessId",
                            e
                        )
                    }
                }
            }
            emitAll(
                materialDao.getAllRecipes(businessId)
                    .map<List<RecipeWithMaterial>, Resource<List<RecipeWithMaterial>>> { Resource.Success(it) }
                    .onStart { emit(Resource.Loading()) }
                    .catch { e ->
                        if (e is CancellationException) throw e
                        Log.e(
                            "MaterialRepositoryImpl",
                            "Room recipe observation failed",
                            e
                        )
                        emit(
                            Resource.Error(
                                ErrorSanitizer.clean(
                                    e.message ?: "Failed to observe recipes"
                                )
                            )
                        )
                    }
            )
        }
    }

    override suspend fun upsertMaterial(
        request: UpsertMaterialRequest,
        businessId: String
    ): Resource<MaterialEntity> = withContext(Dispatchers.IO) {
        try {
            val materialId = request.id ?: UUID.randomUUID().toString()
            val now = Instant.now().toString()
            val existing = request.id?.let { materialDao.getMaterialById(it) }

            val entity = MaterialEntity(
                id = materialId,
                businessId = businessId,
                name  = request.name,
                description  = request.description,
                unit = request.unit,
                quantity = request.quantity ?: 0.0,
                costPerUnit = request.costPerUnit,
                lowStockAlert = request.lowStockAlert,
                status = existing?.status ?: "AVAILABLE",
                supplier = request.supplier,
                notes = request.notes,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = Util.SyncStatus.PENDING
            )
            Log.d("MaterialRepositoryImpl", "upsertMaterial: $entity")
            materialDao.insertMaterial(entity)

            if (networkMonitor.isConnected()) {
                Log.d("MaterialRepositoryImpl", "upsertMaterial: ${entity.toDto()}")
                postgrest.from("materials").upsert(entity.toDto()) { onConflict = "id" }
                Log.d("MaterialRepositoryImpl", "upsertMaterial: ${entity.toDto()}")
                materialDao.markSynced(materialId, now)
            }
            Resource.Success(entity)
        } catch (e: Exception) {
            Resource.Error(ErrorSanitizer.clean(e.message ?: "Failed to save material"))
        }
    }

    override suspend fun updateStatus(
        materialId: String, status: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = materialDao.getMaterialById(materialId)
                ?: return@withContext Resource.Error(ErrorSanitizer.clean("Material not found"))
            val updated = existing.copy(
                status     = status,
                updatedAt  = Instant.now().toString(),
                syncStatus = Util.SyncStatus.DIRTY
            )
            materialDao.insertMaterial(updated)
            if (networkMonitor.isConnected()) {
                Log.d("MaterialRepositoryImpl", "updateStatus: $updated")
                postgrest.from("materials")
                    .update(mapOf("status" to status)) {
                        filter { eq("id", materialId) }
                    }
                Log.d("MaterialRepositoryImpl", "updateStatus: $updated")
                materialDao.markSynced(materialId, updated.updatedAt)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ErrorSanitizer.clean(e.message ?: "Failed"))
        }
    }

    override suspend fun adjustStock(
        materialId: String, quantity: Double,
        notes: String?, staffId: String, businessId: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            if (networkMonitor.isConnected()) {
                Log.d("MaterialRepositoryImpl", "Quantity: $quantity")
                val params = mutableMapOf<String, Any?>(
                    "p_material_id" to materialId,
                    "p_quantity" to quantity,
                    "p_staff_id" to staffId,
                    "p_business_id" to businessId
                )
                if (!notes.isNullOrBlank()) {
                    params["p_notes"] = notes
                }
                postgrest.rpc(
                    function = "adjust_material_stock",
                    parameters = AdjustMaterialStockRequest(
                        materialId = materialId,
                        quantity = quantity,
                        staffId = staffId,
                        businessId = businessId,
                        notes = notes?.takeIf { it.isNotBlank() }
                    )
                )
                Log.d("MaterialRepositoryImpl", "adjustStock: $params")
                pullFromServer(businessId)
            } else {
                val existing = materialDao.getMaterialById(materialId)
                    ?: return@withContext Resource.Error(ErrorSanitizer.clean("Material not found"))

                val newQuantity = existing.quantity + quantity
                val newStatus = when {
                    newQuantity <= 0 -> "OUT_OF_STOCK"
                    existing.lowStockAlert != null && newQuantity <= existing.lowStockAlert -> "LOW_STOCK"
                    else -> "AVAILABLE"
                }

                materialDao.insertMaterial(
                    existing.copy(
                        quantity = newQuantity,
                        status = newStatus,
                        updatedAt = Instant.now().toString(),
                        syncStatus = Util.SyncStatus.DIRTY
                    )
                )
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.d("MaterialRepositoryImpl", "adjustStock: ${e.message}")
            Resource.Error(ErrorSanitizer.clean(e.message ?: "Failed to adjust stock"))
        }
    }

    override suspend fun saveRecipe(
        variantId: String,
        items: List<RecipeItem>,
        businessId: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = Instant.now().toString()
            val existing = materialDao.getRecipesByVariant(variantId)
            val existingByMaterial = existing.associateBy { it.materialId }
            val incomingMaterialIds = items.map { it.materialId }.toSet()

            val toUpsert = mutableListOf<RecipeEntity>()
            val toSoftDelete = mutableListOf<RecipeEntity>()

            items.forEach { item ->
                val match = existingByMaterial[item.materialId]
                when {
                    match == null -> {
                        // Genuinely new line
                        toUpsert.add(
                            RecipeEntity(
                                id = UUID.randomUUID().toString(),
                                businessId = businessId,
                                variantId = variantId,
                                materialId = item.materialId,
                                quantityNeeded = item.quantityNeeded,
                                notes = null,
                                createdAt = now,
                                updatedAt = now,
                                syncStatus = Util.SyncStatus.PENDING
                            )
                        )
                    }
                    match.quantityNeeded != item.quantityNeeded || match.syncStatus == Util.SyncStatus.DELETED -> {
                        // Quantity changed, OR this material was removed and is now being re-added
                        // before the delete synced — resurrect it rather than creating a duplicate row.
                        toUpsert.add(
                            match.copy(
                                quantityNeeded = item.quantityNeeded,
                                updatedAt = now,
                                syncStatus = if (match.syncStatus == Util.SyncStatus.PENDING)
                                    Util.SyncStatus.PENDING else Util.SyncStatus.DIRTY
                            )
                        )
                    }
                    // else: unchanged and already synced — don't touch it
                }
            }

            // Lines present locally but missing from the new list — soft delete, don't remove outright
            existing
                .filter { it.materialId !in incomingMaterialIds && it.syncStatus != Util.SyncStatus.DELETED }
                .forEach { toSoftDelete.add(it.copy(syncStatus = Util.SyncStatus.DELETED, updatedAt = now)) }

            if (toUpsert.isNotEmpty()) materialDao.insertRecipes(toUpsert)
            if (toSoftDelete.isNotEmpty()) materialDao.insertRecipes(toSoftDelete)

            // Best-effort immediate push when online. SyncWorker is the actual guarantee —
            // this just avoids making someone wait for the next sync cycle while connected.
            // A failure here is silently left for SyncWorker to retry, same as everywhere else.
            if (networkMonitor.isConnected()) {
                (toUpsert + toSoftDelete).forEach { entity ->
                    try {
                        when (entity.syncStatus) {
                            Util.SyncStatus.PENDING -> pushNewRecipeItem(entity)
                            Util.SyncStatus.DIRTY   -> pushRecipeUpdate(entity)
                            Util.SyncStatus.DELETED -> pushRecipeDelete(entity.id)
                            Util.SyncStatus.SYNCED  -> Unit
                        }
                    } catch (e: Exception) {
                        Log.w("MaterialRepositoryImpl", "saveRecipe: push failed for ${entity.id}, left for SyncWorker: ${e.message}")
                    }
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ErrorSanitizer.clean(e.message ?: "Failed to save recipe"))
        }
    }

    internal suspend fun pushNewRecipeItem(entity: RecipeEntity) {
        postgrest.from("recipes").insert(
            RecipeInsertRequest(
                id = entity.id,
                businessId = entity.businessId,
                variantId = entity.variantId,
                materialId = entity.materialId,
                quantityNeeded = entity.quantityNeeded,
                notes = entity.notes,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        )

        materialDao.markRecipeSynced(
            entity.id,
            Instant.now().toString()
        )
    }

    internal suspend fun pushRecipeUpdate(entity: RecipeEntity) {
        postgrest.from("recipes")
            .update(
                RecipeUpdateRequest(
                    quantityNeeded = entity.quantityNeeded,
                    updatedAt = entity.updatedAt
                )
            ) {
                filter {
                    eq("id", entity.id)
                }
            }

        materialDao.markRecipeSynced(
            entity.id,
            Instant.now().toString()
        )
    }

    internal suspend fun pushRecipeDelete(recipeId: String) {
        postgrest.from("recipes").delete { filter { eq("id", recipeId) } }
        materialDao.hardDeleteRecipe(recipeId)  // only removed locally once the server confirms
    }

    override suspend fun deleteMaterial(materialId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                materialDao.softDelete(materialId, Instant.now().toString())
                if (networkMonitor.isConnected()) {
                    postgrest.from("materials")
                        .update(mapOf("deleted_at" to Instant.now().toString())) {
                            filter { eq("id", materialId) }
                        }
                    materialDao.hardDelete(materialId)
                }
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(ErrorSanitizer.clean(e.message ?: "Failed"))
            }
        }

    override suspend fun getLowStockCount(businessId: String): Int =
        materialDao.getLowStockCount(businessId)

    private suspend fun mergeRecipes(remote: List<RecipeDto>, localSnapshot: List<RecipeEntity>) {
        val localById = localSnapshot.associateBy { it.id }
        val remoteIds = remote.map { it.id }.toSet()

        var validMaterialIds = materialDao.getAllMaterialIds().toSet()
        val missingMaterialIds = remote.map { it.materialId }.filter { it !in validMaterialIds }.distinct()

        if (missingMaterialIds.isNotEmpty()) {
            try {
                Log.d("MaterialRepositoryImpl", "Fetching missing materials for recipes: $missingMaterialIds")
                val missingMaterials = postgrest.from("materials").select {
                    filter { isIn("id", missingMaterialIds) }
                }.decodeList<MaterialDto>()
                if (missingMaterials.isNotEmpty()) {
                    materialDao.insertMaterials(missingMaterials.map { it.toEntity() })
                    validMaterialIds = materialDao.getAllMaterialIds().toSet()
                }
            } catch (e: Exception) {
                Log.w("MaterialRepositoryImpl", "Failed to fetch missing materials for recipes: ${e.message}")
            }
        }

        val safeToUpsert = remote.mapNotNull { r ->
            if (r.materialId !in validMaterialIds) {
                Log.w(
                    "MaterialRepositoryImpl",
                    "Skipping recipe ${r.id}: material ${r.materialId} not found in local database"
                )
                return@mapNotNull null
            }
            val local = localById[r.id]
            if (local != null && local.syncStatus != Util.SyncStatus.SYNCED) {
                return@mapNotNull null   // preserve local edits not yet pushed
            }
            RecipeEntity(
                id = r.id,
                businessId = r.businessId,
                variantId = r.variantId,
                materialId = r.materialId,
                quantityNeeded = r.quantityNeeded,
                notes = r.notes,
                createdAt = r.createdAt,
                updatedAt = r.updatedAt,
                syncStatus = Util.SyncStatus.SYNCED
            )
        }

        if (safeToUpsert.isNotEmpty()) {
            materialDao.insertRecipes(safeToUpsert)
        }
        localSnapshot
            .filter { it.syncStatus == Util.SyncStatus.SYNCED && it.id !in remoteIds }
            .forEach { materialDao.hardDeleteRecipe(it.id) }
    }

    internal suspend fun pullRecipeForVariant(variantId: String) {
        try {
            val remote = postgrest.from("recipes").select {
                filter { eq("variant_id", variantId) }
            }.decodeList<RecipeDto>()
            Log.d("MaterialRepositoryImpl", "pullRecipeForVariant: $remote")
            Log.d("MaterialRepositoryImpl", "pullRecipeForVariant: ${remote.size}")
            val localSnapshot = materialDao.getRecipesByVariant(variantId)
            mergeRecipes(remote, localSnapshot)
        } catch (e: Exception) {
            Log.w("MaterialRepositoryImpl", "pullRecipeForVariant failed for $variantId: ${e.message}")
        }
    }

    private suspend fun pullAllRecipes(businessId: String) {
        try {
            pullFromServer(businessId)
        } catch (e: Exception) {
            Log.w("MaterialRepositoryImpl", "pullFromServer failed during pullAllRecipes: ${e.message}")
        }

        val pageSize = 500
        var offset = 0
        val allRemote = mutableListOf<RecipeDto>()

        while (true) {
            val page = postgrest.from("recipes").select {
                filter { eq("business_id", businessId) }
                range(from = offset.toLong(), to = (offset + pageSize - 1).toLong())
            }.decodeList<RecipeDto>()
            Log.d("MaterialRepositoryImpl", "pullAllRecipes: $page")
            allRemote += page
            if (page.size < pageSize) break
            offset += pageSize
        }

        val localSnapshot = materialDao.getRecipesForBusiness(businessId)
        mergeRecipes(allRemote, localSnapshot)
    }

    override suspend fun pullFromServer(businessId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            if (businessId.isBlank()) {
                Log.e("MaterialRepositoryImpl", "pullFromServer: businessId is blank")
                return@withContext Resource.Error("Business ID is missing")
            }
            try {
                Log.d("MaterialRepositoryImpl", "pullFromServer: $businessId")
                val remote = postgrest.from("materials").select {
                    filter { eq("business_id", businessId); isNull("deleted_at") }
                }.decodeList<MaterialDto>()
                Log.d("MaterialRepositoryImpl", "pullFromServer: $remote")
                val unsyncedIds = materialDao.getUnsyncedMaterials().map { it.id }.toSet()
                remote.filter { it.id !in unsyncedIds }
                    .map { it.toEntity() }
                    .let { if (it.isNotEmpty()) materialDao.insertMaterials(it) }
                Log.d("MaterialRepositoryImpl", "pullFromServer: $remote")
                val remoteIds = remote.map { it.id }.toSet()
                materialDao.getAllMaterialIds(businessId)
                    .filter { it !in remoteIds && it !in unsyncedIds }
                    .forEach { materialDao.hardDelete(it) }
                Log.d("MaterialRepositoryImpl", "pullFromServer: $remote")
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(ErrorSanitizer.clean(e.message ?: "Pull failed"))
            }
        }
}
