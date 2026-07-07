package com.techsultan.zenithpro.features.payment.data.repository

import com.techsultan.zenithpro.core.data.remote.BusinessDto
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.sales.TransferType
import com.techsultan.zenithpro.features.sales.SaleStatus
import com.techsultan.zenithpro.features.sales.data.local.SaleDao
import com.techsultan.zenithpro.features.settings.data.remote.TerminalDto
import com.techsultan.zenithpro.features.payment.data.local.PaymentDao
import com.techsultan.zenithpro.features.sales.data.local.PaymentEntity
import com.techsultan.zenithpro.features.payment.domain.model.PaymentStatus
import com.techsultan.zenithpro.features.payment.domain.repository.PaymentGateway
import com.techsultan.zenithpro.features.payment.domain.repository.TransferPaymentDetails
import com.techsultan.zenithpro.features.payment.domain.repository.PaymentStatusResult
import com.techsultan.zenithpro.features.payment.data.remote.PaymentDto
import com.techsultan.zenithpro.features.payment.data.remote.toEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

class PaymentRepository(
    private val paymentDao: PaymentDao,
    private val saleDao: SaleDao,
    private val supabaseClient: SupabaseClient,
) : PaymentGateway {

    // ── Observe payment for a sale — Room as source of truth ─────────────────
    // PaymentViewModel collects this Flow to detect webhook confirmation.
    // When nomba-webhook fires and SyncManager pulls the update, this emits.
    fun observePaymentBySaleId(saleId: String): Flow<PaymentEntity?> =
        paymentDao.observePaymentBySaleId(saleId)

    // ── Initiate bank transfer ────────────────────────────────────────────────
    override suspend fun initiateTransfer(
        saleId: String,
        businessId: String,
        terminalId: String,
        amount: Long,
    ): Result<TransferPaymentDetails> = runCatching {

        // Read virtual account from local terminal record (already provisioned)
        // No network call needed here — details are stored on the terminal
        val terminal = supabaseClient.postgrest
            .from("terminals")
            .select {
                filter { eq("id", terminalId) }
            }
            .decodeSingleOrNull<TerminalDto>()
            ?: error("Terminal not found")

        if (terminal.nombaVirtualAccountNumber == null) {
            error("Terminal payment account not configured. Please contact your administrator.")
        }

        // Create local payment record immediately — PENDING
        val paymentId = UUID.randomUUID().toString()
        val now = Instant.now().toString()

        paymentDao.insertPayment(
            PaymentEntity(
                id = paymentId,
                saleId = saleId,
                businessId = businessId,
                provider = "NOMBA",
                status = PaymentStatus.PENDING.name,
                amount = amount,
                currency = "NGN",
                paymentMethod = "TRANSFER",
                syncStatus = Util.SyncStatus.PENDING.name,
                createdAt = now,
                updatedAt = now,
            )
        )

        val currencySymbol = "₦"
        val amountDisplay = "$currencySymbol${String.format("%,.2f", amount / 100.0)}"

        TransferPaymentDetails(
            paymentId = paymentId,
            accountNumber = terminal.nombaVirtualAccountNumber,
            bankName = terminal.nombaVirtualAccountBank ?: "Bank",
            accountName = terminal.nombaVirtualAccountName ?: "ZenithPro",
            amount = amount,
            amountDisplay = amountDisplay,
        )
    }

    // Manual transfer — no virtual account needed, just create payment record
    suspend fun initiateManualTransfer(
        saleId: String,
        businessId: String,
        amount: Long,
        transferType: TransferType,
    ): Result<TransferPaymentDetails> = runCatching {
        val paymentId = UUID.randomUUID().toString()
        val now = Instant.now().toString()

        // Load business bank details from session or business settings
        val business = supabaseClient.postgrest
            .from("businesses")
            .select { filter { eq("id", businessId) } }
            .decodeSingleOrNull<BusinessDto>()
            ?: error("Business not found")

        paymentDao.insertPayment(
            PaymentEntity(
                id = paymentId,
                saleId = saleId,
                businessId = businessId,
                provider = "MANUAL",
                status = PaymentStatus.PENDING.name,
                amount = amount,
                currency = "NGN",
                paymentMethod = "TRANSFER",
                transferType = TransferType.MANUAL.name,
                syncStatus = Util.SyncStatus.PENDING.name,
                createdAt = now,
                updatedAt = now,
            )
        )

        TransferPaymentDetails(
            paymentId = paymentId,
            accountNumber = business.accountNumber ?: "",
            bankName = business.bankName ?: "",
            accountName = business.accountName ?: "",
            amount = amount,
            amountDisplay = "₦${String.format("%,.2f", amount / 100.0)}",
        )
    }

    // Cashier confirms manual payment received
    suspend fun confirmManualPayment(
        paymentId: String,
        saleId: String,
    ): Result<Unit> = runCatching {
        val now = Instant.now().toString()

        // Update payment locally
        paymentDao.updateStatus(paymentId, PaymentStatus.SUCCESSFUL.name, now)

        // Update sale locally — same pattern as webhook does remotely
        // SyncManager will push this to Supabase when online
        saleDao.updateSaleAndPaymentStatus(
            id = saleId,
            paymentStatus = "COMPLETED",
            status = SaleStatus.COMPLETED
        )
    }

    // ── Get payment status from backend ──────────────────────────────────────
    override suspend fun getPaymentStatus(paymentId: String): Result<PaymentStatusResult> =
        runCatching {
            val response = supabaseClient.functions.invoke(
                function = "payments-verify",
                body = buildJsonObject { put("paymentId", paymentId) }
            )
            // Parse response — simplified; add your actual DTO here
            val payment = paymentDao.getPaymentById(paymentId)
                ?: error("Payment not found locally")
            PaymentStatusResult(
                paymentId = payment.id,
                status = payment.status,
                providerReference = payment.providerReference,
            )
        }

    // ── Sync: pull confirmed payments from server ─────────────────────────────
    suspend fun pullPaymentsFromServer(businessId: String) {
        val remotePayments = supabaseClient.postgrest
            .from("payments")
            .select {
                filter { eq("business_id", businessId) }
            }
            .decodeList<PaymentDto>()

        remotePayments.forEach { dto ->
            paymentDao.insertPayment(dto.toEntity())
        }
    }
}
