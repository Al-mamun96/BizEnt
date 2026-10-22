package com.leen.bizentry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leen.bizentry.data.BusinessEntry
import com.leen.bizentry.data.BusinessEntryRepository
import com.leen.bizentry.data.Money
import com.leen.bizentry.data.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

private const val DEFAULT_CURRENCY = "BDT"

sealed interface EntryMessage {
    data object EmptyInput : EntryMessage
    data object AmountInvalid : EntryMessage
    data object Saved : EntryMessage
    data object Updated : EntryMessage
    data object Deleted : EntryMessage
    data class SaveFailed(val detail: String?) : EntryMessage
}

data class BusinessEntryUiState(
    val entries: List<BusinessEntry> = emptyList(),
    val text: String = "",
    val amountInput: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val transactionDateEpochDay: Long = 0L,
    val editingId: Long? = null,
    val isSaving: Boolean = false,
    val message: EntryMessage? = null
)

private data class EntryForm(
    val text: String = "",
    val amountInput: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val transactionDateEpochDay: Long,
    val editingEntry: BusinessEntry? = null
)

class BusinessEntryViewModel(
    private val repository: BusinessEntryRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val form = MutableStateFlow(EntryForm(transactionDateEpochDay = todayEpochDay()))
    private val isSaving = MutableStateFlow(false)
    private val message = MutableStateFlow<EntryMessage?>(null)

    val uiState: StateFlow<BusinessEntryUiState> = combine(
        repository.allEntries,
        form,
        isSaving,
        message
    ) { entries, currentForm, saving, currentMessage ->
        BusinessEntryUiState(
            entries = entries,
            text = currentForm.text,
            amountInput = currentForm.amountInput,
            type = currentForm.type,
            transactionDateEpochDay = currentForm.transactionDateEpochDay,
            editingId = currentForm.editingEntry?.id,
            isSaving = saving,
            message = currentMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BusinessEntryUiState(transactionDateEpochDay = todayEpochDay())
    )

    fun onTextChange(value: String) {
        form.value = form.value.copy(text = value)
        message.value = null
    }

    fun onAmountChange(value: String) {
        form.value = form.value.copy(amountInput = value)
        message.value = null
    }

    fun onDateChange(date: LocalDate) {
        form.value = form.value.copy(transactionDateEpochDay = date.toEpochDay())
        message.value = null
    }

    fun onTypeChange(type: TransactionType) {
        form.value = form.value.copy(type = type)
        message.value = null
    }

    fun startEditing(entry: BusinessEntry) {
        form.value = EntryForm(
            text = entry.rawText,
            amountInput = entry.amountMinor?.let { Money.format(it) } ?: "",
            type = entry.type,
            transactionDateEpochDay = entry.transactionDateEpochDay ?: todayEpochDay(),
            editingEntry = entry
        )
        message.value = null
    }

    fun cancelEditing() {
        form.value = clearedForm(form.value)
        message.value = null
    }

    fun delete(entry: BusinessEntry) {
        if (isSaving.value) return
        isSaving.value = true

        viewModelScope.launch {
            try {
                repository.delete(entry)
                if (form.value.editingEntry?.id == entry.id) {
                    form.value = clearedForm(form.value)
                }
                message.value = EntryMessage.Deleted
            } catch (e: Exception) {
                message.value = EntryMessage.SaveFailed(e.localizedMessage)
            } finally {
                isSaving.value = false
            }
        }
    }

    fun save() {
        val current = form.value
        val trimmed = current.text.trim()

        if (trimmed.isEmpty()) {
            message.value = EntryMessage.EmptyInput
            return
        }

        val amountWasGiven = current.amountInput.isNotBlank()
        val amountMinor = if (amountWasGiven) Money.parseMinorUnits(current.amountInput) else null

        if (amountWasGiven && amountMinor == null) {
            message.value = EntryMessage.AmountInvalid
            return
        }

        if (isSaving.value) return
        isSaving.value = true

        val existing = current.editingEntry

        viewModelScope.launch {
            try {
                if (existing != null) {
                    repository.update(
                        existing.copy(
                            rawText = trimmed,
                            amountMinor = amountMinor,
                            currency = currencyFor(amountMinor, existing.currency),
                            type = current.type,
                            transactionDateEpochDay = current.transactionDateEpochDay,
                            transactionDateZone = clock.zone.id,
                            updatedAt = clock.millis()
                        )
                    )
                } else {
                    repository.add(
                        BusinessEntry(
                            rawText = trimmed,
                            amountMinor = amountMinor,
                            currency = currencyFor(amountMinor, null),
                            type = current.type,
                            transactionDateEpochDay = current.transactionDateEpochDay,
                            transactionDateZone = clock.zone.id,
                            createdAt = clock.millis()
                        )
                    )
                }

                form.value = clearedForm(current)
                message.value = if (existing != null) EntryMessage.Updated else EntryMessage.Saved
            } catch (e: Exception) {
                message.value = EntryMessage.SaveFailed(e.localizedMessage)
            } finally {
                isSaving.value = false
            }
        }
    }

    private fun currencyFor(amountMinor: Long?, existingCurrency: String?): String? =
        if (amountMinor == null) null else existingCurrency ?: DEFAULT_CURRENCY

    private fun clearedForm(current: EntryForm): EntryForm = EntryForm(
        type = current.type,
        transactionDateEpochDay = current.transactionDateEpochDay
    )

    private fun todayEpochDay(): Long = LocalDate.now(clock).toEpochDay()

    companion object {
        fun factory(repository: BusinessEntryRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { BusinessEntryViewModel(repository) }
            }
    }
}
