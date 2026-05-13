package com.techsultan.zenithpro.core.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.techsultan.zenithpro.core.util.Util.PrinterType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.printerDataStore by preferencesDataStore(name = "printer_settings")

class PrinterDataStore(private val context: Context) {

    companion object {
        val PRINTER_ID = stringPreferencesKey("printer_id")
        val PRINTER_NAME = stringPreferencesKey("printer_name")
        val PRINTER_TYPE = stringPreferencesKey("printer_type")
        val PRINTER_ADDRESS = stringPreferencesKey("printer_address")
    }

    suspend fun savePrinter(printer: PrinterDevice) {
        context.printerDataStore.edit { prefs ->
            prefs[PRINTER_ID] = printer.id
            prefs[PRINTER_NAME] = printer.name
            prefs[PRINTER_TYPE] = printer.type.name
            prefs[PRINTER_ADDRESS] = printer.address ?: ""
        }
    }

    val savedPrinter: Flow<PrinterDevice?> = context.printerDataStore.data.map { prefs ->
        val id = prefs[PRINTER_ID] ?: return@map null
        val name = prefs[PRINTER_NAME] ?: return@map null
        val typeStr = prefs[PRINTER_TYPE] ?: return@map null
        val address = prefs[PRINTER_ADDRESS]

        PrinterDevice(
            id = id,
            name = name,
            type = PrinterType.valueOf(typeStr),
            address = address?.ifBlank { null }
        )
    }

    suspend fun clearPrinter() {
        context.printerDataStore.edit { it.clear() }
    }
}
