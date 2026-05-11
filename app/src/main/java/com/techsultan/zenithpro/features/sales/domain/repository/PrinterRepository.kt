package com.techsultan.zenithpro.features.sales.domain.repository

import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.util.Resource


interface PrinterRepository {

    suspend fun printReceipt(
        receipt: ReceiptData
    ): Resource<Unit>

    suspend fun reprintReceipt(
        receipt: ReceiptData
    ): Resource<Unit>
}