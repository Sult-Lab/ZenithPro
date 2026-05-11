package com.techsultan.zenithpro.features.sales.data.repository

import com.dantsu.escposprinter.EscPosPrinter
import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.manager.PrinterManager
import com.techsultan.zenithpro.core.util.ReceiptFormatter
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.sales.domain.repository.PrinterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrinterRepositoryImpl(
    private val formatter: ReceiptFormatter,
    private val printerManager: PrinterManager
) : PrinterRepository {

    override suspend fun printReceipt(
        receipt: ReceiptData
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val printerConnection = printerManager.getConnectedPrinter()
                ?: return@withContext Resource.Error("No printer connected")

            val printer = EscPosPrinter(
                printerConnection,
                203,
                48f,
                32
            )

            val formattedReceipt = formatter.format(receipt)

            printer.printFormattedText(formattedReceipt)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Printing failed")
        }
    }

    override suspend fun reprintReceipt(
        receipt: ReceiptData
    ): Resource<Unit> {
        return printReceipt(receipt)
    }
}