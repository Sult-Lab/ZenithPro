package com.techsultan.zenithpro.features.production.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.production.domain.repository.ProductionRepository

class CompleteProductionOrderUseCase(private val repository: ProductionRepository) {
    suspend operator fun invoke(
        orderId: String,
        branchId: String,
        actualQuantity: Double,
        completedBy: String,
        completedByName: String,
        completedByRole: String,
        customerId: String? = null,
        terminalId: String? = null,
        unitPrice: Long? = null,
        amountPaid: Long? = null,
        paymentMethod: String? = "CASH",
        remarks: String? = null
    ) = repository.completeOrder(
        orderId = orderId,
        branchId = branchId,
        actualQuantity = actualQuantity,
        completedBy = completedBy,
        completedByName = completedByName,
        completedByRole = completedByRole,
        customerId = customerId,
        terminalId = terminalId,
        unitPrice = unitPrice,
        amountPaid = amountPaid,
        paymentMethod = paymentMethod,
        remarks = remarks
    )
}
