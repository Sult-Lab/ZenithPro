package com.techsultan.zenithpro.features.customer.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.data.remote.CustomerRequest
import com.techsultan.zenithpro.features.customer.domain.repository.CustomerRepository
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.data.remote.DebtPaymentRequest
import com.techsultan.zenithpro.features.sales.domain.repository.SaleRepository

class RecordDebtPaymentUseCase(private val repository: SaleRepository) {
    suspend operator fun invoke(
        saleId: String,
        customerId: String,
        amount: Long,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        notes: String? = null
    ): Resource<Unit> {
        if (amount <= 0) return Resource.Error("Payment amount must be greater than zero")
        return repository.recordDebtPayment(
            DebtPaymentRequest(
                saleId = saleId,
                customerId = customerId,
                amount = amount,
                paymentMethod = paymentMethod.name,
                notes = notes
            )
        )
    }
}