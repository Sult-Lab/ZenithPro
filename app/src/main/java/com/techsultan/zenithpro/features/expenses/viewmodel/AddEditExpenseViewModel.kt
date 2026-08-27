package com.techsultan.zenithpro.features.expenses.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.ZenithAnalytics
import androidx.core.os.bundleOf
import com.techsultan.zenithpro.core.util.Util.trimOrNull
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseEntity
import com.techsultan.zenithpro.features.expenses.data.remote.UpsertExpenseRequest
import com.techsultan.zenithpro.features.expenses.domain.repository.ExpenseRepository
import com.techsultan.zenithpro.features.expenses.domain.use_case.UpsertExpenseUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class AddEditExpenseViewModel(
    private val upsertExpenseUseCase: UpsertExpenseUseCase,
    private val expenseRepository: ExpenseRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditExpenseUiState())
    val state: StateFlow<AddEditExpenseUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AddEditExpenseEvent>()
    val events = _events.asSharedFlow()

    val session = sessionManager.currentSession

    fun loadForEdit(expense: ExpenseEntity) {
        _state.update {
            it.copy(
                expenseId   = expense.id,
                title       = expense.title,
                amount      = (expense.amount / 100.0).toString(),
                category    = expense.category,
                notes       = expense.notes ?: "",
                expenseDate = LocalDate.parse(expense.expenseDate),
                isEditMode  = true
            )
        }
    }

    fun loadCategories(businessId: String) {
        viewModelScope.launch {
            val cats = expenseRepository.getCategories(businessId)
            _state.update { it.copy(existingCategories = cats) }
        }
    }

    fun onTitleChanged(value: String)       { _state.update { it.copy(title = value, titleError = null) } }
    fun onAmountChanged(value: String)      { _state.update { it.copy(amount = value, amountError = null) } }
    fun onCategoryChanged(value: String)    { _state.update { it.copy(category = value, categoryError = null) } }
    fun onNotesChanged(value: String)       { _state.update { it.copy(notes = value) } }
    fun onDateChanged(date: LocalDate)      { _state.update { it.copy(expenseDate = date) } }

    fun save(businessId: String, staffId: String) {
        val s = _state.value

        // Validate
        var hasError = false
        if (s.title.isBlank()) {
            _state.update { it.copy(titleError = "Title is required") }
            hasError = true
        }
        val amountLong = s.amount.replace(",", "").toDoubleOrNull()
            ?.let { (it * 100).toLong() }
        if (amountLong == null || amountLong <= 0) {
            _state.update { it.copy(amountError = "Enter a valid amount") }
            hasError = true
        }
        if (s.category.isBlank()) {
            _state.update { it.copy(categoryError = "Category is required") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val result = upsertExpenseUseCase(
                request = UpsertExpenseRequest(
                    id = s.expenseId ?: UUID.randomUUID().toString(),
                    title = s.title.trim(),
                    amount = amountLong!!,
                    category = s.category.trim(),
                    notes = s.notes.trimOrNull(),
                    expenseDate = s.expenseDate.toString(),
                    branchId = null
                ),
                businessId = businessId,
                staffId    = staffId
            )

            _state.update { it.copy(isLoading = false) }
            when (result) {
                is Resource.Success -> {
                    if (!s.isEditMode) {
                        ZenithAnalytics.trackEvent("expense_created", bundleOf(
                            "category" to s.category,
                            "amount_kobo" to (amountLong ?: 0L)
                        ))
                    }
                    _events.emit(AddEditExpenseEvent.Saved)
                }
                is Resource.Error -> {
                    ZenithAnalytics.logError(Exception(result.message), context = "AddEditExpenseViewModel.save")
                    _events.emit(AddEditExpenseEvent.ShowError(result.message ?: "Save failed"))
                }
                else -> Unit
            }
        }
    }

    sealed class AddEditExpenseEvent {
        data object Saved : AddEditExpenseEvent()
        data class ShowError(val message: String) : AddEditExpenseEvent()
    }
}

data class AddEditExpenseUiState(
    val expenseId: String? = null,
    val title: String = "",
    val amount: String = "",
    val category: String = "",
    val notes: String = "",
    val expenseDate: LocalDate = LocalDate.now(),
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val existingCategories: List<String> = emptyList(),
    val titleError: String? = null,
    val amountError: String? = null,
    val categoryError: String? = null
)