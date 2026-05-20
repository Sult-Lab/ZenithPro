package com.techsultan.zenithpro.features.customer.data.mapper

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.customer.data.remote.DebtPaymentDto
import com.techsultan.zenithpro.features.customer.data.local.DebtPaymentEntity

fun DebtPaymentDto.toEntity() = DebtPaymentEntity(
    id = id,
    businessId = businessId,
    saleId = saleId,
    customerId = customerId,
    staffId = staffId,
    amount = amount,
    paymentMethod = paymentMethod,
    notes = notes,
    paidAt = paidAt,
    syncStatus = Util.SyncStatus.SYNCED
)