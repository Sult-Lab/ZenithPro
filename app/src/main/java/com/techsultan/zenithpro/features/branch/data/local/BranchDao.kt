package com.techsultan.zenithpro.features.branch.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BranchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: BranchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranches(branches: List<BranchEntity>)

    @Query("""
        SELECT * FROM branches
        WHERE businessId = :businessId
          AND deletedAt  IS NULL
        ORDER BY name ASC
    """)
    fun getBranches(businessId: String): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches WHERE id = :id")
    suspend fun getBranchById(id: String): BranchEntity?

    @Query("SELECT * FROM branches WHERE syncStatus IN ('PENDING','DIRTY','DELETED')")
    suspend fun getUnsyncedBranches(): List<BranchEntity>

    @Query("UPDATE branches SET syncStatus = 'SYNCED', updatedAt = :at WHERE id = :id")
    suspend fun markSynced(id: String, at: String)

    @Query("UPDATE branches SET deletedAt = :at, syncStatus = 'DELETED' WHERE id = :id")
    suspend fun softDelete(id: String, at: String)

    @Query("DELETE FROM branches WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT id FROM branches WHERE businessId = :businessId")
    suspend fun getAllBranchIds(businessId: String): List<String>
}