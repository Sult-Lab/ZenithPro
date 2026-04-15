package com.techsultan.zenithpro.features.sales.domain.use_case

import android.util.Log
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.data.remote.CartItem
import com.techsultan.zenithpro.features.sales.data.remote.ProcessSaleRequest
import com.techsultan.zenithpro.features.sales.data.remote.ProcessSaleResponse
import com.techsultan.zenithpro.features.sales.data.remote.SaleItemRequest
import com.techsultan.zenithpro.features.sales.domain.repository.SaleRepository
import java.util.UUID

class ProcessSaleUseCase(private val repository: SaleRepository) {
    suspend operator fun invoke(
        cart: List<CartItem>,
        customerId: String?,
        branchId: String?,
        staffId: String,
        amountPaid: Long,
        paymentMethod: PaymentMethod,
        discountAmount: Long = 0L,
        taxAmount: Long = 0L,
        notes: String? = null
    ): Resource<ProcessSaleResponse> {
        if (cart.isEmpty()) return Resource.Error("Cart is empty")
        if (amountPaid < 0) return Resource.Error("Invalid payment amount")

        val subtotal = cart.sumOf { it.totalPrice }
        val total    = maxOf(0L, subtotal - discountAmount + taxAmount)

        if (paymentMethod == PaymentMethod.DEBT && customerId == null) {
            return Resource.Error("Select a customer for debt sales")
        }


        val effectiveAmountPaid = when (paymentMethod) {
            PaymentMethod.CASH,
            PaymentMethod.TRANSFER -> if (amountPaid <= 0L) total else amountPaid
            PaymentMethod.DEBT     -> 0L
            PaymentMethod.SPLIT    -> amountPaid
            PaymentMethod.POS -> if (amountPaid <= 0L) total else amountPaid
            PaymentMethod.USSD -> if (amountPaid <= 0L) total else amountPaid
            PaymentMethod.CARD -> if (amountPaid <= 0L) total else amountPaid
        }

        val change = maxOf(0L, effectiveAmountPaid - total)

        Log.d("ProcessSaleUseCase",
            "total=$total effectiveAmountPaid=$effectiveAmountPaid " +
                    "change=$change method=${paymentMethod.name}"
        )

        val request = ProcessSaleRequest(
            clientTransactionId = UUID.randomUUID().toString(),
            branchId            = branchId,
            customerId          = customerId,
            staffId             = staffId,
            items               = cart.map { item ->
                SaleItemRequest(
                    variantId   = item.variantId,
                    productId   = item.productId,
                    productName = item.productName,
                    variantSku  = item.variantSku,
                    unitPrice   = item.unitPrice,
                    costPrice   = item.costPrice,
                    quantity    = item.quantity,
                    discount    = item.discount
                )
            },
            subtotal            = subtotal,
            discountAmount      = discountAmount,
            taxAmount           = taxAmount,
            totalAmount         = total,
            amountPaid          = effectiveAmountPaid,
            changeAmount        = change,
            paymentMethod       = paymentMethod.name,
            notes               = notes
        )

        return repository.processSale(request, cart)
    }
}