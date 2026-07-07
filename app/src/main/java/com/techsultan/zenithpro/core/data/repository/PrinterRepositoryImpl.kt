package com.techsultan.zenithpro.core.data.repository

import com.techsultan.zenithpro.core.data.local.PrinterDevice
import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.domain.repository.PrinterRepository
import com.techsultan.zenithpro.core.manager.PrinterDriverFactory
import com.techsultan.zenithpro.core.util.ReceiptFormatter
import kotlinx.coroutines.delay

class PrinterRepositoryImpl(
    private val formatter: ReceiptFormatter,
    private val driverFactory: PrinterDriverFactory
) : PrinterRepository {

    override suspend fun printReceipt(
        receipt: ReceiptData,
        printer: PrinterDevice
    ): Result<Unit> {

        return runCatching {
            val driver = driverFactory.create(printer.type)
            driver.connect(printer).getOrThrow()
            driver.print(formatter.format(receipt)).getOrThrow()
            delay(1500)
            driver.disconnect()
        }
    }

    override suspend fun printCustom(
        content: String,
        printer: PrinterDevice
    ): Result<Unit> {
        return runCatching {
            val driver = driverFactory.create(printer.type)
            driver.connect(printer).getOrThrow()
            driver.print(content).getOrThrow()
            delay(500)
            driver.disconnect()
        }
    }
}
