package com.techsultan.zenithpro.features.payment.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.util.ZenithAnalytics
import androidx.core.os.bundleOf
import com.techsultan.zenithpro.features.payment.data.repository.PaymentRepository
import com.techsultan.zenithpro.features.payment.domain.repository.TransferPaymentDetails
import com.techsultan.zenithpro.features.payment.domain.model.PaymentStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


class TransferPaymentViewModel(
    private val paymentRepository: PaymentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransferPaymentUiState>(
        TransferPaymentUiState.Idle
    )
    val uiState: StateFlow<TransferPaymentUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TransferPaymentEvent>()
    val events = _events.asSharedFlow()

    private var observeJob: Job? = null
    private var pollJob: Job? = null



    private suspend fun handleNombaTransfer(
        saleId: String,
        businessId: String,
        terminalId: String?,
        amount: Long,
    ) {
        if (terminalId == null) {
            _uiState.value = TransferPaymentUiState.Error(
                "No terminal configured for this branch. Contact your administrator."
            )
            return
        }

        paymentRepository.initiateTransfer(
            saleId = saleId,
            businessId = businessId,
            terminalId = terminalId,
            amount = amount,
        ).fold(
            onSuccess = { details ->
                ZenithAnalytics.trackEvent("transfer_payment_selected", bundleOf(
                    "transfer_type" to "NOMBA"
                ))
                _uiState.value = TransferPaymentUiState.AwaitingNombaPayment(details)
                startObservingPayment(details.paymentId, saleId)
                startPolling(details.paymentId, saleId, businessId)
            },
            onFailure = { error ->
                _uiState.value = TransferPaymentUiState.Error(
                    error.message ?: "Unable to initiate payment"
                )
            }
        )
    }

    // Called when cashier taps "Confirm Payment Received" in manual flow
    fun confirmManualPayment(paymentId: String, saleId: String) {
        viewModelScope.launch {
            paymentRepository.confirmManualPayment(paymentId, saleId)
                .fold(
                    onSuccess = {
                        ZenithAnalytics.trackEvent("transfer_payment_selected", bundleOf(
                            "transfer_type" to "MANUAL"
                        ))
                        _uiState.value = TransferPaymentUiState.Confirmed
                        _events.emit(
                            TransferPaymentEvent.PaymentConfirmed(saleId, paymentId)
                        )
                    },
                    onFailure = {
                        _uiState.value = TransferPaymentUiState.Error(
                            "Failed to confirm payment. Please try again."
                        )
                    }
                )
        }
    }

    // Observe Room for status change — fires when SyncManager pulls webhook update
    private fun startObservingPayment(paymentId: String, saleId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            paymentRepository.observePaymentBySaleId(saleId)
                .collect { payment ->
                    payment ?: return@collect
                    val status = PaymentStatus.valueOf(payment.status)
                    when {
                        status.isSuccess -> {
                            ZenithAnalytics.trackEvent("nomba_transfer_confirmed", bundleOf(
                                "amount_kobo" to (payment.amount)
                            ))
                            _uiState.value = TransferPaymentUiState.Confirmed
                            _events.emit(TransferPaymentEvent.PaymentConfirmed(saleId, paymentId))
                            cancelJobs()
                        }
                        status == PaymentStatus.FAILED -> {
                            _uiState.value = TransferPaymentUiState.Failed
                            _events.emit(TransferPaymentEvent.PaymentFailed)
                            cancelJobs()
                        }
                        else -> { /* still waiting */ }
                    }
                }
        }
    }

    // Fallback poll — in case sync is slow or connectivity was interrupted
    private fun startPolling(paymentId: String, saleId: String, businessId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            repeat(18) { // poll for up to 3 minutes (18 × 10s)
                delay(10_000.milliseconds)
                paymentRepository.pullPaymentsFromServer(businessId)
                // observePaymentBySaleId() will pick up any change automatically
            }
            // Timeout after 3 minutes
            if (_uiState.value is TransferPaymentUiState.AwaitingPayment) {
                _uiState.value = TransferPaymentUiState.Timeout
                _events.emit(TransferPaymentEvent.PaymentTimeout)
                cancelJobs()
            }
        }
    }

    fun cancelPayment() {
        cancelJobs()
        viewModelScope.launch {
            _events.emit(TransferPaymentEvent.PaymentCancelled)
        }
    }

    private fun cancelJobs() {
        observeJob?.cancel()
        pollJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        cancelJobs()
    }
}


sealed class TransferPaymentUiState {
    object Idle : TransferPaymentUiState()
    object Loading : TransferPaymentUiState()

    // Base trait for states that are waiting for money
    interface AwaitingPayment

    // Nomba — waiting for webhook to auto-confirm
    data class AwaitingNombaPayment(
        val details: TransferPaymentDetails
    ) : TransferPaymentUiState(), AwaitingPayment

    // Manual — cashier sees account details, taps confirm when alert arrives
    data class AwaitingManualConfirmation(
        val details: TransferPaymentDetails
    ) : TransferPaymentUiState(), AwaitingPayment

    object Confirmed : TransferPaymentUiState()
    object Failed : TransferPaymentUiState()
    object Timeout : TransferPaymentUiState()
    data class Error(val message: String) : TransferPaymentUiState()
}


sealed class TransferPaymentEvent {
    data class PaymentConfirmed(val saleId: String, val paymentId: String) : TransferPaymentEvent()
    object PaymentFailed : TransferPaymentEvent()
    object PaymentCancelled : TransferPaymentEvent()
    object PaymentTimeout : TransferPaymentEvent()
}
