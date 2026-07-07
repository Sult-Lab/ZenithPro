package com.techsultan.zenithpro.core.manager

import android.content.Context
import com.techsultan.zenithpro.core.data.BluetoothPrinterDriver
import com.techsultan.zenithpro.core.data.EmbeddedPrinterDriver
import com.techsultan.zenithpro.core.data.UsbPrinterDriver
import com.techsultan.zenithpro.core.domain.PrinterDriver
import com.techsultan.zenithpro.core.util.Util.PrinterType

class PrinterDriverFactory(
    private val context: Context
) {

    fun create(
        type: PrinterType
    ): PrinterDriver {

        return when(type) {

            PrinterType.BLUETOOTH -> {
                BluetoothPrinterDriver(context)
            }

            PrinterType.USB -> {
                UsbPrinterDriver(context)
            }

            PrinterType.EMBEDDED -> {
                EmbeddedPrinterDriver(context)
            }
        }
    }
}
