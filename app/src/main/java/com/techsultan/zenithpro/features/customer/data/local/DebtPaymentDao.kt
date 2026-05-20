package com.techsultan.zenithpro.features.customer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtPaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: DebtPaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<DebtPaymentEntity>)

    @Query("""
        SELECT * FROM debt_payments
        WHERE customerId = :customerId
        ORDER BY paidAt DESC
    """)
    fun getPaymentsForCustomer(customerId: String): Flow<List<DebtPaymentEntity>>

    @Query("""
        SELECT * FROM debt_payments
        WHERE saleId = :saleId
        ORDER BY paidAt DESC
    """)
    suspend fun getPaymentsForSale(saleId: String): List<DebtPaymentEntity>

    @Query("""
        SELECT * FROM debt_payments
        WHERE businessId = :businessId
        ORDER BY paidAt DESC
    """)
    fun getAllPayments(businessId: String): Flow<List<DebtPaymentEntity>>
}