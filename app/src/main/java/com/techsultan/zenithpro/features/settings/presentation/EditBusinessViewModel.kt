package com.techsultan.zenithpro.features.settings.presentation

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.BusinessConstants
import com.techsultan.zenithpro.core.util.ImageCacheManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util.trimOrNull
import com.techsultan.zenithpro.features.settings.domain.use_case.UpdateBusinessUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class EditBusinessViewModel(
    private val updateBusinessUseCase: UpdateBusinessUseCase,
    private val imageCacheManager: ImageCacheManager,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(EditBusinessUiState())
    val state: StateFlow<EditBusinessUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<EditBusinessEvent>()
    val events = _events.asSharedFlow()

    init {
        sessionManager.currentSession?.let { session ->
            _state.update {
                it.copy(
                    businessName = session.businessName,
                    businessType = session.businessType ?: "",
                    phoneNumber = session.businessPhone ?: "",
                    emailAddress = session.businessEmail ?: "",
                    physicalAddress = session.businessAddress ?: "",
                    existingLogoUrl = session.businessLogoUrl,
                    selectedCurrency = BusinessConstants.CURRENCIES
                        .firstOrNull { c -> c.code == session.currencyCode }
                        ?: BusinessConstants.CURRENCIES.first()
                )
            }
        }
    }

    fun onNameChanged(value: String) {
        _state.update { it.copy(businessName = value, nameError = null) }
    }

    fun onTypeChanged(value: String) {
        _state.update { it.copy(businessType = value) }
    }

    fun onPhoneChanged(value: String) {
        _state.update { it.copy(phoneNumber = value) }
    }

    fun onEmailChanged(value: String) {
        _state.update { it.copy(emailAddress = value) }
    }

    fun onAddressChanged(value: String) {
        _state.update { it.copy(physicalAddress = value) }
    }

    fun onCurrencyChanged(currency: BusinessConstants.CurrencyOption) {
        _state.update { it.copy(selectedCurrency = currency) }
    }

    fun onLogoSelected(uri: Uri) {
        _state.update { it.copy(newLogoUri = uri) }
    }

    fun onRemoveLogo() {
        _state.update { it.copy(newLogoUri = null, existingLogoUrl = null) }
    }

    fun save() {
        val s = _state.value
        if (s.businessName.isBlank()) {
            _state.update { it.copy(nameError = "Business name is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Cache logo if a new one was picked
            val cachedLogoUri = s.newLogoUri?.let { uri ->
                try {
                    Uri.fromFile(File(imageCacheManager.cacheImage(uri)))
                } catch (e: Exception) {
                    Log.e("EditBusinessVM", "Failed to cache logo: ${e.message}")
                    null
                }
            }

            val result = updateBusinessUseCase(
                name           = s.businessName,
                type           = s.businessType.trimOrNull(),
                phone          = s.phoneNumber.trimOrNull(),
                email          = s.emailAddress.trimOrNull(),
                address        = s.physicalAddress.trimOrNull(),
                currencyCode   = s.selectedCurrency.code,
                currencySymbol = s.selectedCurrency.symbol,
                existingLogoUrl = s.existingLogoUrl,
                logoUri        = cachedLogoUri
            )

            when (result) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _events.emit(EditBusinessEvent.Saved)
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                    _events.emit(EditBusinessEvent.ShowError(result.message ?: "Failed"))
                }
                else -> _state.update { it.copy(isLoading = false) }
            }
        }
    }

    sealed class EditBusinessEvent {
        data object Saved : EditBusinessEvent()
        data class ShowError(val message: String) : EditBusinessEvent()
    }
}

data class EditBusinessUiState(
    val businessName: String    = "",
    val businessType: String    = "",
    val phoneNumber: String     = "",
    val emailAddress: String    = "",
    val physicalAddress: String = "",
    val selectedCurrency: BusinessConstants.CurrencyOption =
        BusinessConstants.CURRENCIES.first(),
    val existingLogoUrl: String? = null,
    val newLogoUri: Uri?         = null,
    val isLoading: Boolean       = false,
    val nameError: String?       = null,
    val error: String?           = null
) {
    val logoDisplayUri: Any? get() = newLogoUri ?: existingLogoUrl
}