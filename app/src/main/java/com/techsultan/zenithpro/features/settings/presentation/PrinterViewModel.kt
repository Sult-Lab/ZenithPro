package com.techsultan.zenithpro.features.settings.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.techsultan.zenithpro.core.data.local.PrinterDataStore
import com.techsultan.zenithpro.core.data.local.PrinterDevice
import com.techsultan.zenithpro.core.util.Util.PrinterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class PrinterViewModel(
    private val printerDataStore: PrinterDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(PrinterUiState())
    val state: StateFlow<PrinterUiState> = _state.asStateFlow()

    init {
        loadSavedPrinter()
    }

    private fun loadSavedPrinter() {
        viewModelScope.launch {
            val saved = printerDataStore.savedPrinter.first()
            Log.d(
                "PRINTER",
                "Loaded printer: $saved"
            )
            _state.update { it.copy(selectedPrinter = saved) }
        }
    }

    fun discoverBluetoothPrinters() {

        viewModelScope.launch {

            _state.update {
                it.copy(isScanning = true)
            }

            try {

                val printers = withContext(Dispatchers.IO) {

                    BluetoothPrintersConnections()
                        .list
                }

                val printerDevices = printers?.map {

                    PrinterDevice(
                        id = it.device.address,
                        name = it.device.name ?: "Unknown Printer",
                        type = PrinterType.BLUETOOTH,
                        address = it.device.address
                    )
                } ?: emptyList()

                _state.update {
                    it.copy(
                        availablePrinters = printerDevices,
                        isScanning = false
                    )
                }

            } catch (e: Exception) {

                _state.update {
                    it.copy(
                        isScanning = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun selectPrinter(printer: PrinterDevice) {
        Log.d(
            "PRINTER",
            "Saving printer: $printer"
        )
        viewModelScope.launch {
            printerDataStore.savePrinter(printer)
            _state.update { it.copy(selectedPrinter = printer) }
        }
    }
    
    fun useEmbeddedPrinter() {
        val printer = PrinterDevice(
            id = "embedded",
            name = "Embedded Printer",
            type = PrinterType.EMBEDDED
        )
        selectPrinter(printer)
    }

    fun clearPrinter() {
        viewModelScope.launch {
            printerDataStore.clearPrinter()
            _state.update { it.copy(selectedPrinter = null) }
        }
    }
}

data class PrinterUiState(
    val availablePrinters: List<PrinterDevice> = emptyList(),
    val selectedPrinter: PrinterDevice? = null,
    val isScanning: Boolean = false,
    val error: String? = null
)
