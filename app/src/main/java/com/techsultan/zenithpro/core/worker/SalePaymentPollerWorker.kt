package com.techsultan.zenithpro.core.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.techsultan.zenithpro.core.manager.NotificationHelper
import com.techsultan.zenithpro.features.sales.data.remote.SaleDto
import io.github.jan.supabase.postgrest.Postgrest
import java.util.concurrent.TimeUnit

class SalePaymentPollerWorker(
    context: Context,
    params: WorkerParameters,
    private val postgrest: Postgrest
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val saleId     = inputData.getString(KEY_SALE_ID)     ?: return Result.failure()
        val businessId = inputData.getString(KEY_BUSINESS_ID) ?: return Result.failure()
        Log.d("PaymentPoller", "Polling for saleId=$saleId")
        return try {
            val sale = postgrest
                .from("sales")
                .select { filter { eq("id", saleId); eq("business_id", businessId) } }
                .decodeSingleOrNull<SaleDto>()

            when (sale?.paymentStatus) {
                "COMPLETED" -> {
                    // Notify via local notification + cancel further work
                    notifyPaymentConfirmed(saleId, sale.totalAmount)
                    cancel(applicationContext, saleId)
                    Result.success()
                }
                "FAILED", "EXPIRED" -> {
                    notifyPaymentFailed(saleId)
                    cancel(applicationContext, saleId)
                    Result.success()
                }
                else -> {
                    // Still AWAITING_PAYMENT — retry
                    if (runAttemptCount >= MAX_ATTEMPTS) {
                        notifyPaymentTimeout(saleId)
                        Result.success()  // Give up gracefully
                    } else {
                        Result.retry()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("SalePoller", "Poll attempt $runAttemptCount failed: ${e.message}")
            if (runAttemptCount >= MAX_ATTEMPTS) Result.success() else Result.retry()
        }
    }

    private fun notifyPaymentConfirmed(saleId: String, amount: Long) {
        // Local notification so cashier knows even if they navigated away
        NotificationHelper.showPaymentConfirmed(applicationContext, saleId, amount)
    }

    private fun notifyPaymentFailed(saleId: String) {
        NotificationHelper.showPaymentFailed(applicationContext, saleId)
    }

    private fun notifyPaymentTimeout(saleId: String) {
        NotificationHelper.showPaymentTimeout(applicationContext, saleId)
    }

    companion object {
        const val KEY_SALE_ID     = "sale_id"
        const val KEY_BUSINESS_ID = "business_id"
        const val MAX_ATTEMPTS    = 72   // 72 × 5s backoff ≈ 6 minutes

        fun schedule(context: Context, saleId: String, businessId: String) {
            val data = workDataOf(
                KEY_SALE_ID     to saleId,
                KEY_BUSINESS_ID to businessId
            )

            val request = PeriodicWorkRequestBuilder<SalePaymentPollerWorker>(
                repeatInterval        = 10,
                repeatIntervalTimeUnit = TimeUnit.SECONDS
            )
                .setInputData(data)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    5,
                    TimeUnit.SECONDS
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag(pollerTag(saleId))
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                pollerTag(saleId),
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d("SalePoller", "Scheduled poller for sale $saleId")
        }

        fun cancel(context: Context, saleId: String) {
            WorkManager.getInstance(context).cancelAllWorkByTag(pollerTag(saleId))
            Log.d("SalePoller", "Cancelled poller for sale $saleId")
        }

        fun pollerTag(saleId: String) = "sale_poller_$saleId"
    }
}