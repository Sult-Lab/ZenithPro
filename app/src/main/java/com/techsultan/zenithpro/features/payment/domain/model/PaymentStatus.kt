package com.techsultan.zenithpro.features.payment.domain.model

enum class PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESSFUL,
    FAILED,
    CANCELLED,
    EXPIRED,
    REFUNDED;

    val isTerminal: Boolean
        get() = this in listOf(SUCCESSFUL, FAILED, CANCELLED, EXPIRED, REFUNDED)

    val isSuccess: Boolean
        get() = this == SUCCESSFUL
}
