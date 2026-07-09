package com.techsultan.zenithpro.features.sales.domain.model

enum class NombaStatus {
    PENDING, SUCCESS, FAILED
}

data class NombaTransfer(
    val id: String,
    val senderName: String,
    val bankName: String,
    val amount: Long, // in kobo
    val status: NombaStatus,
    val timestamp: String,
)
