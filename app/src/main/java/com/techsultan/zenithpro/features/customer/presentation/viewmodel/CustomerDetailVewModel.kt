package com.techsultan.zenithpro.features.customer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.data.local.DebtPaymentDao
import com.techsultan.zenithpro.features.customer.data.local.DebtPaymentEntity
import com.techsultan.zenithpro.features.customer.domain.use_case.GetCustomerDetailUseCase
import com.techsultan.zenithpro.features.customer.domain.use_case.RecordDebtPaymentUseCase
import com.techsultan.zenithpro.features.customer.util.CustomerTransaction
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.data.local.SaleDao
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CustomerDetailViewModel(
    private val getCustomerDetailUseCase: GetCustomerDetailUseCase,
    private val recordDebtPaymentUseCase: RecordDebtPaymentUseCase,
    private val sessionManager: SessionManager,
    private val debtPaymentDao: DebtPaymentDao,
    private val saleDao: SaleDao,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerDetailUiState())
    val state: StateFlow<CustomerDetailUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CustomerDetailEvent>()
    val events = _events.asSharedFlow()

    val session get() = sessionManager.currentSession

    fun init(customerId: String, businessId: String) {
        viewModelScope.launch {
            launch {
                getCustomerDetailUseCase(customerId).collect { result ->
                    if (result is Resource.Success) {
                        _state.update { it.copy(customer = result.data) }
                    }
                }
            }

            launch {
                combine(
                    // Sales for this customer
                    flow {
                        emit(saleDao.getSalesByCustomer(customerId))
                    },
                    // Debt payments for this customer
                    debtPaymentDao.getPaymentsForCustomer(customerId)
                ) { sales, payments ->
                    buildTransactionTimeline(sales, payments)
                }.collect { transactions ->
                    _state.update { it.copy(transactions = transactions) }
                }
            }
        }
    }

    private fun buildTransactionTimeline(
        sales: List<SaleWithItems>,
        payments: List<DebtPaymentEntity>
    ): List<CustomerTransaction> {
        val salesMap = sales.associate { it.sale.id to it.sale }

        val saleTx = sales.map { CustomerTransaction.Sale(it) }
        val paymentTx = payments.map { payment ->
            CustomerTransaction.DebtPayment(
                payment = payment,
                originalSale = salesMap[payment.saleId]
            )
        }

        // Merge and sort by date descending
        return (saleTx + paymentTx)
            .sortedByDescending { it.sortDate }
    }
    fun recordPayment(
        saleId: String,
        amount: Long,
        paymentMethod: PaymentMethod,
        notes: String?
    ) {
        val customer = _state.value.customer ?: return
        viewModelScope.launch {
            _state.update { it.copy(isPaymentLoading = true) }
            val result = recordDebtPaymentUseCase(
                saleId        = saleId,
                customerId    = customer.id,
                amount        = amount,
                paymentMethod = paymentMethod,
                notes         = notes
            )
            _state.update { it.copy(isPaymentLoading = false) }
            when (result) {
                is Resource.Success ->
                    _events.emit(CustomerDetailEvent.PaymentRecorded)
                is Resource.Error ->
                    _events.emit(CustomerDetailEvent.ShowError(result.message ?: "Payment failed"))
                else -> Unit
            }
        }
    }

    sealed class CustomerDetailEvent {
        data object PaymentRecorded : CustomerDetailEvent()
        data class ShowError(val message: String) : CustomerDetailEvent()
    }
}

data class CustomerDetailUiState(
    val customer: CustomerEntity? = null,
    val isPaymentLoading: Boolean = false,
    val error: String? = null,
    val transactions: List<CustomerTransaction>  = emptyList(),
)
