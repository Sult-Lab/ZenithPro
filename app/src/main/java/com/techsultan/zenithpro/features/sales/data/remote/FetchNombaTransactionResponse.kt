package com.techsultan.zenithpro.features.sales.data.remote

import com.techsultan.zenithpro.features.sales.domain.model.NombaStatus
import com.techsultan.zenithpro.features.sales.domain.model.NombaTransfer
import kotlinx.serialization.Serializable

@Serializable
data class FetchNombaTransactionsResponse(
    val results: List<NombaTransactionApiDto> = emptyList(),
)

@Serializable
data class NombaTransactionApiDto(
    val id: String,
    val terminalId: String = "",
    val terminalName: String = "",
    val virtualAccountNumber: String = "",
    val amount: Long = 0L,               // kobo
    val senderName: String = "Unknown Sender",
    val bankName: String = "Unknown Bank",
    val narration: String = "",
    val status: String = "SUCCESS",
    val timeCreated: String = "",
    val sessionId: String = "",
    val entryType: String = "CREDIT",
    val confirmed: Boolean = false,
) {
    fun toDomain(): NombaTransfer = NombaTransfer(
        id         = id,
        senderName = senderName.trim().ifBlank { "Unknown Sender" },
        bankName   = bankName,
        amount     = amount,
        status     = if (confirmed) NombaStatus.SUCCESS else NombaStatus.PENDING,
        timestamp  = formatRelativeTime(timeCreated),
    )

    private fun formatRelativeTime(isoTime: String): String {
        return try {
            val then    = java.time.Instant.parse(isoTime)
            val now     = java.time.Instant.now()
            val minutes = java.time.temporal.ChronoUnit.MINUTES.between(then, now)
            val hours   = java.time.temporal.ChronoUnit.HOURS.between(then, now)
            val days    = java.time.temporal.ChronoUnit.DAYS.between(then, now)
            when {
                minutes < 1  -> "Just now"
                minutes < 60 -> "$minutes mins ago"
                hours < 24   -> "$hours ${if (hours == 1L) "hour" else "hours"} ago"
                days == 1L   -> "Yesterday"
                else -> java.time.format.DateTimeFormatter
                    .ofPattern("d MMM")
                    .withZone(java.time.ZoneId.systemDefault())
                    .format(then)
            }
        } catch (e: Exception) {
            isoTime
        }
    }
}