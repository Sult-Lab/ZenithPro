package com.techsultan.zenithpro.core.manager

import android.content.Context
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections

class PrinterManager(
    private val context: Context
) {

    fun getConnectedPrinter(): BluetoothConnection? {

        val printers = BluetoothPrintersConnections.selectFirstPaired()

        return printers
    }
}