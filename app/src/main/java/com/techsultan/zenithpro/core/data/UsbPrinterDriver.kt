package com.techsultan.zenithpro.core.data

import android.content.Context
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.techsultan.zenithpro.core.data.local.PrinterDevice
import com.techsultan.zenithpro.core.domain.PrinterDriver

class UsbPrinterDriver(
    private val context: Context
) : PrinterDriver {

    private var connection: UsbConnection? = null

    override suspend fun connect(device: PrinterDevice): Result<Unit> {
        return try {
            val usbConnection = UsbPrintersConnections.selectFirstConnected(context)
            if (usbConnection != null) {
                connection = usbConnection
                Result.success(Unit)
            } else {
                Result.failure(Exception("USB printer not found or permission denied"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun print(formattedText: String): Result<Unit> {
        return try {
            if (isConnected()) {
                // Printing logic would go here, typically using EscPosPrinter(connection, ...)
                Result.success(Unit)
            } else {
                Result.failure(Exception("USB printer not connected"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        connection = null
    }

    override fun isConnected(): Boolean {
        return connection != null
    }
}
