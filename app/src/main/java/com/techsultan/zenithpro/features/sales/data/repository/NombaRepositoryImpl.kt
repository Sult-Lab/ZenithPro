package com.techsultan.zenithpro.features.sales.data.repository

import android.util.Log
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.sales.data.remote.FetchNombaTransactionsResponse
import com.techsultan.zenithpro.features.sales.data.remote.NombaTransactionDto
import com.techsultan.zenithpro.features.sales.domain.model.NombaStatus
import com.techsultan.zenithpro.features.sales.domain.model.NombaSummary
import com.techsultan.zenithpro.features.sales.domain.model.NombaTransfer
import com.techsultan.zenithpro.features.sales.domain.repository.NombaRepository
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.call.body
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.round
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class NombaRepositoryImpl(
    private val postgrest: Postgrest,
    private val functions: Functions,
) : NombaRepository {

    override fun getNombaTransfers(
        businessId: String
    ): Flow<Resource<List<NombaTransfer>>> = flow {
        emit(Resource.Loading())
        try {
            // Get current terminal from session/local DB
            val terminal = postgrest
                .from("terminals")
                .select {
                    filter {
                        eq("business_id", businessId)
                        eq("is_active", true)
                    }
                    limit(1)
                }
                .decodeList<Map<String, String?>>()
                .firstOrNull()

            val terminalId = terminal?.get("id") ?: run {
                emit(Resource.Error("No active terminal found"))
                return@flow
            }

            val response = functions.invoke(
                function = "fetch-nomba-transactions",
                body = buildJsonObject {
                    put("businessId", businessId)
                    put("terminalId", terminalId)
                }
            )

            val json = kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            }
            val result = json.decodeFromString<FetchNombaTransactionsResponse>(
                response.body<String>()
            )

            emit(Resource.Success(result.results.map { it.toDomain() }))

        } catch (e: Exception) {
            Log.e("NombaRepo", "getNombaTransfers failed: ${e.message}", e)

            // Fallback to local nomba_transactions table
            try {
                val rows = postgrest
                    .from("nomba_transactions")
                    .select {
                        filter { eq("business_id", businessId) }
                        order("paid_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(100)
                    }
                    .decodeList<NombaTransactionDto>()

                emit(Resource.Success(rows.map { it.toDomain() }))
            } catch (_: Exception) {
                emit(Resource.Error(e.message ?: "Failed to load transfers"))
            }
        }
    }

    override fun getNombaSummary(
        businessId: String
    ): Flow<Resource<NombaSummary>> = flow {
        emit(Resource.Loading())
        try {
            // Today's transactions
            val todayStart = Instant.now()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toString()

            val todayRows = postgrest
                .from("nomba_transactions")
                .select {
                    filter {
                        eq("business_id", businessId)
                        gte("paid_at", todayStart)
                    }
                }
                .decodeList<NombaTransactionDto>()

            val totalToday = todayRows.sumOf { it.amount }

            // Yesterday's total for percentage change
            val yesterdayStart = Instant.now()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .minusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toString()

            val yesterdayRows = postgrest
                .from("nomba_transactions")
                .select {
                    filter {
                        eq("business_id", businessId)
                        gte("paid_at", yesterdayStart)
                        lt("paid_at", todayStart)
                    }
                }
                .decodeList<NombaTransactionDto>()

            val totalYesterday = yesterdayRows.sumOf { it.amount }

            val percentageChange = if (totalYesterday > 0) {
                ((totalToday - totalYesterday).toDouble() / totalYesterday * 100)
                    .let { round(it * 10.0) / 10.0 }
            } else if (totalToday > 0) {
                100.0
            } else {
                0.0
            }

            // Get terminal info from first transaction
            val terminal = postgrest
                .from("terminals")
                .select {
                    filter { eq("business_id", businessId) }
                    limit(1)
                }
                .decodeList<Map<String, String?>>()
                .firstOrNull()

            val summary = NombaSummary(
                totalReceivedToday = totalToday,
                percentageChange   = percentageChange,
                isConnected        = true,
                terminalName       = terminal?.get("name") ?: "Main Terminal",
                terminalId         = terminal?.get("id")?.take(8) ?: "—",
            )

            emit(Resource.Success(summary))
        } catch (e: Exception) {
            Log.e("NombaRepo", "getNombaSummary failed: ${e.message}", e)
            emit(Resource.Error(e.message ?: "Failed to load summary"))
        }
    }

    override suspend fun confirmTransfer(transferId: String): Resource<Unit> {
        return try {
            postgrest
                .from("nomba_transactions")
                .update({ set("confirmed", true) }) {
                    filter { eq("id", transferId) }
                }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("NombaRepo", "confirmTransfer failed: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to confirm transfer")
        }
    }

    override suspend fun refreshTransfers(businessId: String): Resource<Unit> {
        return Resource.Success(Unit) // pull-to-refresh calls loadData() again
    }

    private fun NombaTransactionDto.toDomain(): NombaTransfer {
        return NombaTransfer(
            id         = id,
            senderName = senderName?.trim()?.ifBlank { "Unknown Sender" } ?: "Unknown Sender",
            bankName   = bankName ?: "Unknown Bank",
            amount     = amount, // kobo
            status     = if (confirmed) NombaStatus.SUCCESS else NombaStatus.PENDING,
            timestamp  = formatRelativeTime(paidAt),
        )
    }

    private fun formatRelativeTime(isoTime: String): String {
        return try {
            val then    = Instant.parse(isoTime)
            val now     = Instant.now()
            val minutes = ChronoUnit.MINUTES.between(then, now)
            val hours   = ChronoUnit.HOURS.between(then, now)
            val days    = ChronoUnit.DAYS.between(then, now)
            when {
                minutes < 1  -> "Just now"
                minutes < 60 -> "$minutes mins ago"
                hours < 24   -> "$hours ${if (hours == 1L) "hour" else "hours"} ago"
                days == 1L   -> "Yesterday"
                else         -> DateTimeFormatter
                    .ofPattern("d MMM")
                    .withZone(ZoneId.systemDefault())
                    .format(then)
            }
        } catch (_: Exception) {
            isoTime
        }
    }
}
