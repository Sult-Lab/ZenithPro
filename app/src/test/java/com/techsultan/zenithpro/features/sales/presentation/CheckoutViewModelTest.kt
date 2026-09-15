package com.techsultan.zenithpro.features.sales.presentation

import com.techsultan.zenithpro.core.manager.ReceiptNumberGenerator
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.AnalyticsHelper
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.customer.domain.repository.CustomerRepository
import com.techsultan.zenithpro.features.customer.domain.use_case.GetCustomerDetailUseCase
import com.techsultan.zenithpro.features.inventory.data.local.ProductEntity
import com.techsultan.zenithpro.features.inventory.data.local.ProductVariantEntity
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.inventory.data.local.VariantWithStock
import com.techsultan.zenithpro.features.inventory.domain.use_case.GetProductsUseCase
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.domain.use_case.GenerateReceiptUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.ProcessSaleUseCase
import com.techsultan.zenithpro.features.settings.domain.use_case.GetSettingsUseCase
import com.techsultan.zenithpro.features.settings.domain.use_case.GetTerminalsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: CheckoutViewModel
    private val getProductsUseCase: GetProductsUseCase = mock()
    private val processSaleUseCase: ProcessSaleUseCase = mock()
    private val sessionManager: SessionManager = mock()
    private val customerRepository: CustomerRepository = mock()
    private val getCustomerDetailUseCase: GetCustomerDetailUseCase = mock()
    private val generateReceiptUseCase: GenerateReceiptUseCase = mock()
    private val receiptNumberGenerator: ReceiptNumberGenerator = mock()
    private val getSettingsUseCase: GetSettingsUseCase = mock()
    private val getTerminalsUseCase: GetTerminalsUseCase = mock()
    private val analytics: AnalyticsHelper = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        whenever(sessionManager.sessionFlow).thenReturn(flowOf(null))
        whenever(sessionManager.activeBranchId).thenReturn(MutableStateFlow("branch_123"))
        whenever(sessionManager.activeBranchName).thenReturn(MutableStateFlow("Main Branch"))
        whenever(getSettingsUseCase.invoke(any())).thenReturn(flowOf(null))
        whenever(getTerminalsUseCase.invoke(any())).thenReturn(flowOf(Resource.Success(emptyList())))
        
        // Mock sessionManager.businessId
        whenever(sessionManager.businessId).thenReturn("business_123")
        whenever(sessionManager.userId).thenReturn("user_123")

        viewModel = CheckoutViewModel(
            getProductsUseCase,
            processSaleUseCase,
            sessionManager,
            customerRepository,
            getCustomerDetailUseCase,
            generateReceiptUseCase,
            receiptNumberGenerator,
            getSettingsUseCase,
            getTerminalsUseCase,
            analytics
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `clientTransactionId should be stable across state updates`() = runTest {
        val initialId = viewModel.state.value.clientTransactionId
        
        viewModel.onSearchQueryChanged("test")
        
        assertEquals(initialId, viewModel.state.value.clientTransactionId)
    }

    @Test
    fun `clientTransactionId should change when cart is cleared`() = runTest {
        val initialId = viewModel.state.value.clientTransactionId
        
        viewModel.clearCart()
        
        assertNotEquals(initialId, viewModel.state.value.clientTransactionId)
    }

    @Test
    fun `checkout should pass the same clientTransactionId from state`() = runTest {
        // Arrange
        val product = createSampleProduct("p1", "Product 1")
        viewModel.addToCart(product)
        
        val currentState = viewModel.state.value
        val txId = currentState.clientTransactionId
        
        // Act
        viewModel.checkout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(processSaleUseCase).invoke(
            cart = any(),
            customerId = anyOrNull(),
            branchId = anyOrNull(),
            staffId = any(),
            amountPaid = any(),
            paymentMethod = any(),
            discountAmount = any(),
            taxAmount = any(),
            notes = anyOrNull(),
            clientTransactionId = eq(txId)
        )
    }

    private fun createSampleProduct(id: String, name: String): ProductWithVariants {
        val product = ProductEntity(
            id = id,
            businessId = "b1",
            name = name,
            description = null,
            category = "cat1",
            baseSalesPrice = 1000L,
            baseCostPrice = 800L,
            isActive = true,
            imageUrls = emptyList(),
            expiryWarningDays = null,
            updatedAt = "",
            deletedAt = null,
            branchId = "br1"
        )
        val variant = ProductVariantEntity(
            id = "v_$id",
            productId = id,
            businessId = "b1",
            sku = "sku_$id",
            salesPrice = 1000L,
            costPrice = 800L,
            barcode = null,
            updatedAt = "",
            deletedAt = null
        )
        return ProductWithVariants(
            product = product,
            variants = listOf(VariantWithStock(variant, emptyList(), emptyList()))
        )
    }
}
