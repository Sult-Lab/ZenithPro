package com.techsultan.zenithpro.core.domain.repository

import com.techsultan.zenithpro.core.data.local.PrinterDevice
import com.techsultan.zenithpro.core.data.local.ReceiptData

interface PrinterRepository {

    suspend fun printReceipt(
        receipt: ReceiptData,
        printer: PrinterDevice
    ): Result<Unit>

    suspend fun printCustom(
        content: String,
        printer: PrinterDevice
    ): Result<Unit>
}
