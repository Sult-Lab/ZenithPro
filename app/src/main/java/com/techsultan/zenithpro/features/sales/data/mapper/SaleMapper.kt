package com.techsultan.zenithpro.features.sales.data.mapper

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.SaleStatus
import com.techsultan.zenithpro.features.sales.data.local.SaleEntity
import com.techsultan.zenithpro.features.sales.data.local.SaleItemEntity
import com.techsultan.zenithpro.features.sales.data.remote.SaleDto
import com.techsultan.zenithpro.features.sales.data.remote.SaleItemDto

fun SaleDto.toEntity() = SaleEntity(
    id = id,
    clientTransactionId = clientTransactionId,
    businessId = businessId,
    branchId = branchId,
    terminalId = terminalId,
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
    paymentStatus = paymentStatus,
    paymentReference = paymentReference,
    nombaPaymentReference = nombaPaymentReference,
    paymentConfirmedAt = paymentConfirmedAt,
    virtualAccountNumber = virtualAccountNumber,
    virtualAccountBank = virtualAccountBank,
    virtualAccountName = virtualAccountName,
    notes = notes,
    soldAt = soldAt,
    syncStatus = Util.SyncStatus.SYNCED
)

fun SaleItemDto.toEntity() = SaleItemEntity(
    id = id,
    saleId = saleId,
    variantId = variantId,
    productId = productId,
    productName = productName,
    variantSku = variantSku,
    unitPrice = unitPrice,
    costPrice = costPrice,
    quantity = quantity,
    discount = discount,
    totalPrice = totalPrice
)
