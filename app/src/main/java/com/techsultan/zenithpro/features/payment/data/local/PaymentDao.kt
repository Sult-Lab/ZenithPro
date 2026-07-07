package com.techsultan.zenithpro.features.payment.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.techsultan.zenithpro.features.sales.data.local.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getPaymentById(id: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE saleId = :saleId")
    suspend fun getPaymentBySaleId(saleId: String): PaymentEntity?

    // Observe payment status changes — used by PaymentViewModel to detect
    // when webhook confirms the transfer and status flips to SUCCESSFUL
    @Query("SELECT * FROM payments WHERE saleId = :saleId")
    fun observePaymentBySaleId(saleId: String): Flow<PaymentEntity?>

    @Query("SELECT * FROM payments WHERE businessId = :businessId AND syncStatus = 'PENDING'")
    suspend fun getPendingPayments(businessId: String): List<PaymentEntity>

    @Query("UPDATE payments SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: String)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePayment(id: String)
}
