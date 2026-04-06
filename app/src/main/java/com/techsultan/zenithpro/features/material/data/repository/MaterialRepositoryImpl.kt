package com.techsultan.zenithpro.features.material.data.repository

import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.material.data.local.MaterialDao
import com.techsultan.zenithpro.features.material.data.local.MaterialEntity
import com.techsultan.zenithpro.features.material.data.local.RecipeEntity
import com.techsultan.zenithpro.features.material.data.local.RecipeWithMaterial
import com.techsultan.zenithpro.features.material.data.remote.RecipeItem
import com.techsultan.zenithpro.features.material.data.mapper.toDto
import com.techsultan.zenithpro.features.material.data.mapper.toEntity
import com.techsultan.zenithpro.features.material.data.remote.MaterialDto
import com.techsultan.zenithpro.features.material.data.remote.UpsertMaterialRequest
import com.techsultan.zenithpro.features.material.domain.repository.MaterialRepository
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Objects.isNull
import java.util.UUID

class MaterialRepositoryImpl(
    private val materialDao: MaterialDao,
    private val postgrest: Postgrest,
    private val networkMonitor: NetworkMonitor,
) : MaterialRepository {

    override fun getMaterials(businessId: String, status: String?) =
        materialDao.getMaterials(businessId, status)
            .map<List<MaterialEntity>, Resource<List<MaterialEntity>>> { Resource.Success(it) }
            .catch { emit(Resource.Error(it.message ?: "Failed")) }
            .onStart { emit(Resource.Loading()) }

    override fun getLowStockMaterials(businessId: String) =
        materialDao.getLowStockMaterials(businessId)
            .map<List<MaterialEntity>, Resource<List<MaterialEntity>>> { Resource.Success(it) }
            .catch { emit(Resource.Error(it.message ?: "Failed")) }

    override fun getRecipeForVariant(variantId: String) =
        materialDao.getRecipeForVariant(variantId)
            .map<List<RecipeWithMaterial>, Resource<List<RecipeWithMaterial>>> { Resource.Success(it) }
            .catch { emit(Resource.Error(it.message ?: "Failed")) }

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
                quantity = existing?.quantity ?: 0.0,
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
            materialDao.insertMaterial(entity)

            if (networkMonitor.isConnected()) {
                postgrest.from("materials").upsert(entity.toDto()) { onConflict = "id" }
                materialDao.markSynced(materialId, now)
            }
            Resource.Success(entity)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save material")
        }
    }

    override suspend fun updateStatus(
        materialId: String, status: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = materialDao.getMaterialById(materialId)
                ?: return@withContext Resource.Error("Material not found")
            val updated = existing.copy(
                status     = status,
                updatedAt  = Instant.now().toString(),
                syncStatus = Util.SyncStatus.DIRTY
            )
            materialDao.insertMaterial(updated)
            if (networkMonitor.isConnected()) {
                postgrest.from("materials")
                    .update(mapOf("status" to status)) {
                        filter { eq("id", materialId) }
                    }
                materialDao.markSynced(materialId, updated.updatedAt)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed")
        }
    }

    override suspend fun adjustStock(
        materialId: String, quantity: Double,
        notes: String?, staffId: String, businessId: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = materialDao.getMaterialById(materialId)
                ?: return@withContext Resource.Error("Material not found")

            val newQuantity = existing.quantity + quantity
            val newStatus = when {
                newQuantity <= 0                                    -> "OUT_OF_STOCK"
                existing.lowStockAlert != null &&
                        newQuantity <= existing.lowStockAlert               -> "LOW_STOCK"
                else                                                -> "AVAILABLE"
            }

            materialDao.insertMaterial(
                existing.copy(
                    quantity   = newQuantity,
                    status     = newStatus,
                    updatedAt  = Instant.now().toString(),
                    syncStatus = Util.SyncStatus.DIRTY
                )
            )

            if (networkMonitor.isConnected()) {
                postgrest.from("material_movements").insert(
                    mapOf(
                        "business_id"   to businessId,
                        "material_id"   to materialId,
                        "movement_type" to if (quantity > 0) "PURCHASE" else "ADJUSTMENT",
                        "quantity"      to quantity,
                        "notes"         to notes,
                        "recorded_by"   to staffId
                    )
                )
                postgrest.from("materials")
                    .update(mapOf("quantity" to newQuantity, "status" to newStatus)) {
                        filter { eq("id", materialId) }
                    }
                materialDao.markSynced(materialId, Instant.now().toString())
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to adjust stock")
        }
    }

    override suspend fun saveRecipe(
        variantId: String,
        items: List<RecipeItem>,
        businessId: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            // Replace entire recipe
            materialDao.deleteRecipeForVariant(variantId)
            val now = Instant.now().toString()
            val entities = items.map { item ->
                RecipeEntity(
                    id = UUID.randomUUID().toString(),
                    businessId = businessId,
                    variantId = variantId,
                    materialId = item.materialId,
                    quantityNeeded = item.quantityNeeded,
                    notes = null,
                    createdAt = now
                )
            }
            materialDao.insertRecipes(entities)

            if (networkMonitor.isConnected()) {
                // Delete existing and reinsert on server
                postgrest.from("recipes")
                    .delete { filter { eq("variant_id", variantId) } }
                if (entities.isNotEmpty()) {
                    postgrest.from("recipes").insert(
                        entities.map { r ->
                            mapOf(
                                "id"              to r.id,
                                "business_id"     to r.businessId,
                                "variant_id"      to r.variantId,
                                "material_id"     to r.materialId,
                                "quantity_needed" to r.quantityNeeded
                            )
                        }
                    )
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save recipe")
        }
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
                Resource.Error(e.message ?: "Failed")
            }
        }

    override suspend fun getLowStockCount(businessId: String): Int =
        materialDao.getLowStockCount(businessId)

    override suspend fun pullFromServer(businessId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val remote = postgrest.from("materials").select {
                    filter { eq("business_id", businessId); isNull("deleted_at") }
                }.decodeList<MaterialDto>()

                val unsyncedIds = materialDao.getUnsyncedMaterials().map { it.id }.toSet()
                remote.filter { it.id !in unsyncedIds }
                    .map { it.toEntity() }
                    .let { if (it.isNotEmpty()) materialDao.insertMaterials(it) }

                val remoteIds = remote.map { it.id }.toSet()
                materialDao.getAllMaterialIds(businessId)
                    .filter { it !in remoteIds && it !in unsyncedIds }
                    .forEach { materialDao.hardDelete(it) }

                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Pull failed")
            }
        }
}