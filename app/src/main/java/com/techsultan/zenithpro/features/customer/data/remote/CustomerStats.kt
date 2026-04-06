package com.techsultan.zenithpro.features.customer.data.remote

data class CustomerStats(
    val totalCustomers: Int  = 0,
    val totalRevenue: Long   = 0L,
    val totalDebt: Long      = 0L,
    val totalVisits: Int     = 0
)