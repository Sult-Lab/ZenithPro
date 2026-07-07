package com.techsultan.zenithpro.features.settings.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TerminalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerminal(terminal: TerminalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerminals(terminals: List<TerminalEntity>)

    @Query("SELECT * FROM terminals WHERE businessId = :businessId AND isActive = 1")
    fun getTerminals(businessId: String): Flow<List<TerminalEntity>>

    @Query("SELECT * FROM terminals WHERE id = :id")
    suspend fun getTerminalById(id: String): TerminalEntity?

    @Query("""
        UPDATE terminals SET
            nombaSweepBankCode = :bankCode,
            nombaSweepAccountNumber = :accountNumber,
            nombaSweepAccountName = :accountName,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateSweepDetails(
        id: String,
        bankCode: String,
        accountNumber: String,
        accountName: String,
        updatedAt: String
    )

    @Query("DELETE FROM terminals WHERE id = :id")
    suspend fun deleteTerminal(id: String)

    @Query("""
    SELECT * FROM terminals 
    WHERE branchId = :branchId 
    AND isActive = 1 
    ORDER BY createdAt ASC 
    LIMIT 1
""")
    suspend fun getTerminalByBranchId(branchId: String): TerminalEntity?
}