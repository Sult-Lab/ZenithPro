package com.techsultan.zenithpro.features.sales.data.mapper

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.SaleStatus
import com.techsultan.zenithpro.features.sales.data.local.SaleEntity
import com.techsultan.zenithpro.features.sales.data.remote.SaleDto

fun SaleDto.toEntity() = SaleEntity(
    id = id,
    clientTransactionId = clientTransactionId,
    businessId = businessId,
    branchId = branchId,
    customerId = customerId,
    staffId = staffId,
    subtotal = subtotal,
    discountAmount = discountAmount,
    taxAmount = taxAmount,
    totalAmount = totalAmount,
    amountPaid = amountPaid,
    changeAmount = changeAmount,
    debtAmount = debtAmount,
    paymentMethod = PaymentMethod.valueOf(paymentMethod),
    status = SaleStatus.valueOf(status),
    notes = notes,
    soldAt = soldAt,
    syncStatus = Util.SyncStatus.SYNCED
)