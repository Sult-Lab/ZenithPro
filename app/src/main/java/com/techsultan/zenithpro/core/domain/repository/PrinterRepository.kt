package com.techsultan.zenithpro.core.domain.repository

import com.techsultan.zenithpro.core.data.local.PrinterDevice
import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.util.Resource

interface PrinterRepository {

    suspend fun printReceipt(
        receipt: ReceiptData,
        printer: PrinterDevice
    ): Result<Unit>
}