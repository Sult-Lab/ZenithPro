package com.techsultan.zenithpro.features.payment.domain.repository

/**
 * PaymentGateway — abstraction over payment providers.
 *
 * Nomba is the initial implementation. Future providers (Paystack, Flutterwave)
 * plug in here without touching any other layer.
 *
 * The Android app never calls Nomba directly — only through this interface,
 * which delegates to the backend via Supabase Edge Functions.
 */
interface PaymentGateway {

    /**
     * Initiate a bank transfer payment for a sale.
     * Returns the virtual account details to show the customer.
     */
    suspend fun initiateTransfer(
        saleId: String,
        businessId: String,
        terminalId: String,
        amount: Long,
    ): Result<TransferPaymentDetails>

    /**
     * Poll payment status from the backend.
     * Used as a fallback if the local Room record hasn't updated via sync yet.
     */
    suspend fun getPaymentStatus(paymentId: String): Result<PaymentStatusResult>
}

data class TransferPaymentDetails(
    val paymentId: String,
    val accountNumber: String,
    val bankName: String,
    val accountName: String,
    val amount: Long,           // in kobo
    val amountDisplay: String,  // "₦5,400.00"
)

data class PaymentStatusResult(
    val paymentId: String,
    val status: String,         // PaymentStatus enum name
    val providerReference: String?,
)
