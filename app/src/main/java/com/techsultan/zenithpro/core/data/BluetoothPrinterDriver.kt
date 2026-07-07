package com.techsultan.zenithpro.core.data

import android.content.Context
import android.util.Log
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.techsultan.zenithpro.core.data.local.PrinterDevice
import com.techsultan.zenithpro.core.domain.PrinterDriver

class BluetoothPrinterDriver(
    private val context: Context
) : PrinterDriver {

    private var connection: BluetoothConnection? = null

    override suspend fun connect(
        device: PrinterDevice
    ): Result<Unit> {

        return runCatching {

            Log.d("PRINTER", "Starting printer connection")

            val printers =
                BluetoothPrintersConnections()
                    .list

            Log.d(
                "PRINTER",
                "Found printers: ${printers?.size}"
            )

            printers?.forEach {

                Log.d(
                    "PRINTER",
                    "Printer found: ${it.device.name} | ${it.device.address}"
                )
            }

            val bluetoothDevice =
                printers?.firstOrNull {
                    it.device.address == device.address
                }

            Log.d(
                "PRINTER",
                "Selected printer: ${bluetoothDevice?.device?.name}"
            )

            if (bluetoothDevice == null) {

                throw Exception(
                    "Bluetooth printer not found"
                )
            }

            connection = bluetoothDevice.connect()

            Log.d(
                "PRINTER",
                "Connection established: ${connection?.isConnected}"
            )
        }
    }

    override suspend fun print(
        formattedText: String
    ): Result<Unit> {

        return runCatching {

            val activeConnection =
                connection
                    ?: throw Exception(
                        "Printer connection is null"
                    )

            Log.d(
                "PRINTER",
                "Starting actual print"
            )

            Log.d(
                "PRINTER",
                "Formatted text:\n$formattedText"
            )

            val printer = EscPosPrinter(
                activeConnection,
                203,
                48f,
                32
            )

            printer.printFormattedText(
                formattedText
            )

            Log.d(
                "PRINTER",
                "Print command sent successfully"
            )
        }
    }

    override suspend fun disconnect() {

        Log.d(
            "PRINTER",
            "Disconnecting printer"
        )

        connection?.disconnect()

        connection = null
    }

    override fun isConnected(): Boolean {

        val connected =
            connection?.isConnected == true

        Log.d(
            "PRINTER",
            "isConnected = $connected"
        )

        return connected
    }
}
