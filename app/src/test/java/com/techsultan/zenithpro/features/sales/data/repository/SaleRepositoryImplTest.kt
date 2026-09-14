package com.techsultan.zenithpro.features.sales.data.repository

import android.content.Context
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.AnalyticsHelper
import com.techsultan.zenithpro.features.branch.data.local.BranchDao
import com.techsultan.zenithpro.features.customer.data.local.DebtPaymentDao
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.data.local.SaleDao
import com.techsultan.zenithpro.features.sales.data.local.SaleEntity
import com.techsultan.zenithpro.features.sales.data.remote.ProcessSaleRequest
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(RobolectricTestRunner::class)
class SaleRepositoryImplTest {

    private lateinit var repository: SaleRepositoryImpl
    private val context: Context = mock()
    private val saleDao: SaleDao = mock()
    private val functions: Functions = mock()
    private val postgrest: Postgrest = mock()
    private val networkMonitor: NetworkMonitor = mock()
    private val sessionManager: SessionManager = mock()
    private val debtPaymentDao: DebtPaymentDao = mock()
    private val branchDao: BranchDao = mock()
    private val analytics: AnalyticsHelper = mock()

    @Before
    fun setUp() {
        whenever(sessionManager.businessId).thenReturn("b1")
        repository = SaleRepositoryImpl(
            context,
            saleDao,
            functions,
            postgrest,
            networkMonitor,
            sessionManager,
            debtPaymentDao,
            branchDao,
            analytics
        )
    }

    @Test
    fun `processSale should use clientTransactionId as saleId for local storage`() = runTest {
        // Arrange
        val txId = "tx_stable_123"
        val request = ProcessSaleRequest(
            clientTransactionId = txId,
            branchId = "br1",
            customerId = "c1",
            items = emptyList(),
            subtotal = 1000L,
            discountAmount = 0L,
            taxAmount = 0L,
            totalAmount = 1000L,
            amountPaid = 1000L,
            changeAmount = 0L,
            paymentMethod = PaymentMethod.CASH.name,
            notes = null,
            staffId = "s1"
        )
        
        whenever(networkMonitor.isConnected()).thenReturn(false) // Force local only for this test

        // Act
        repository.processSale(request, emptyList())

        // Assert
        val saleCaptor = argumentCaptor<SaleEntity>()
        verify(saleDao).insertSale(saleCaptor.capture())
        assertEquals(txId, saleCaptor.firstValue.id)
        assertEquals(txId, saleCaptor.firstValue.clientTransactionId)
    }
}
