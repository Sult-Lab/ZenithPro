package com.techsultan.zenithpro.features.sales.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.data.local.PrinterDataStore
import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.domain.repository.PrinterRepository
import com.techsultan.zenithpro.core.util.BusinessLogoManager
import com.techsultan.zenithpro.core.util.FileShareManager
import com.techsultan.zenithpro.core.util.ReceiptImageGenerator
import com.techsultan.zenithpro.core.util.ReceiptPdfGenerator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "ReceiptViewModel"

class ReceiptViewModel(
    private val printerRepository: PrinterRepository,
    private val printerDataStore: PrinterDataStore,
    private val receiptPdfGenerator: ReceiptPdfGenerator,
    private val receiptImageGenerator: ReceiptImageGenerator,
    private val fileShareManager: FileShareManager,
    private val logoManager: BusinessLogoManager
) : ViewModel() {

    private val _receipt = MutableStateFlow<ReceiptData?>(null)
    val receipt = _receipt.asStateFlow()

    val businessLogo = logoManager.logo

    private val _events = MutableSharedFlow<ReceiptEvent>()
    val events = _events.asSharedFlow()

    fun setReceipt(receipt: ReceiptData) {
        _receipt.value = receipt
    }

    fun printReceipt(receipt: ReceiptData) {
        viewModelScope.launch {
            try {
                val printer = printerDataStore.savedPrinter.first()
                if (printer == null) {
                    _events.emit(ReceiptEvent.Error("No printer configured"))
                    return@launch
                }
                val result = printerRepository.printReceipt(receipt, printer)
                if (result.isFailure) {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Printing failed"
                    _events.emit(ReceiptEvent.Error(errorMsg))
                } else {
                    _events.emit(ReceiptEvent.PrintSuccess)
                }
            } catch (e: Exception) {
                _events.emit(ReceiptEvent.Error(e.message ?: "An unexpected error occurred"))
            }
        }
    }

    fun shareReceiptPdf(receipt: ReceiptData) {
        viewModelScope.launch {
            runCatching {
                val pdfFile = receiptPdfGenerator.generatePdf(receipt)
                fileShareManager.share(
                    file = pdfFile,
                    mimeType = "application/pdf",
                    title = "Share Receipt PDF"
                )
            }.onFailure {
                Log.e(TAG, "Share PDF error: ${it.message}")
                _events.emit(ReceiptEvent.Error(it.message ?: "Failed to share PDF"))
            }
        }
    }

    fun shareReceiptImage(receipt: ReceiptData) {
        viewModelScope.launch {
            runCatching {
                val imageFile = receiptImageGenerator.generateImage(receipt)
                fileShareManager.share(
                    file = imageFile,
                    mimeType = "image/jpeg",
                    title = "Share Receipt Image"
                )
            }.onFailure {
                Log.e(TAG, "Share image error: ${it.message}")
                _events.emit(ReceiptEvent.Error(it.message ?: "Failed to share image"))
            }
        }
    }

    sealed class ReceiptEvent {
        data class Error(val message: String) : ReceiptEvent()
        object PrintSuccess : ReceiptEvent()
    }
}
