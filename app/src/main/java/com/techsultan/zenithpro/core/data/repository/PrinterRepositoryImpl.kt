package com.techsultan.zenithpro.core.data.repository

import android.util.Log
import com.dantsu.escposprinter.EscPosPrinter
import com.techsultan.zenithpro.core.data.local.PrinterDevice
import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.manager.PrinterManager
import com.techsultan.zenithpro.core.util.ReceiptFormatter
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.domain.repository.PrinterRepository
import com.techsultan.zenithpro.core.manager.PrinterDriverFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class PrinterRepositoryImpl(
    private val formatter: ReceiptFormatter,
    private val driverFactory: PrinterDriverFactory
) : PrinterRepository {

    override suspend fun printReceipt(
        receipt: ReceiptData,
        printer: PrinterDevice
    ): Result<Unit> {

        return runCatching {

            val driver =
                driverFactory.create(
                    printer.type
                )

            driver.connect(printer)
                .getOrThrow()

            driver.print(
                formatter.format(receipt)
            ).getOrThrow()

            delay(1500)

            driver.disconnect()
        }
    }
}