package com.leen.bizentry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leen.bizentry.data.BusinessEntry
import com.leen.bizentry.data.BusinessEntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EntryMessage {
    data object EmptyInput : EntryMessage
    data object Saved : EntryMessage
    data class SaveFailed(val detail: String?) : EntryMessage
}

data class BusinessEntryUiState(
    val entries: List<BusinessEntry> = emptyList(),
    val text: String = "",
    val isSaving: Boolean = false,
    val message: EntryMessage? = null
)

class BusinessEntryViewModel(
    private val repository: BusinessEntryRepository
) : ViewModel() {

    private val text = MutableStateFlow("")
    private val isSaving = MutableStateFlow(false)
    private val message = MutableStateFlow<EntryMessage?>(null)

    val uiState: StateFlow<BusinessEntryUiState> = combine(
        repository.allEntries,
        text,
        isSaving,
        message
    ) { entries, currentText, saving, currentMessage ->
        BusinessEntryUiState(
            entries = entries,
            text = currentText,
            isSaving = saving,
            message = currentMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BusinessEntryUiState()
    )

    fun onTextChange(value: String) {
        text.value = value
        message.value = null
    }

    fun save() {
        val trimmed = text.value.trim()

        if (trimmed.isEmpty()) {
            message.value = EntryMessage.EmptyInput
            return
        }

        if (isSaving.value) return
        isSaving.value = true

        viewModelScope.launch {
            try {
                repository.add(BusinessEntry(rawText = trimmed))
                text.value = ""
                message.value = EntryMessage.Saved
            } catch (e: Exception) {
                message.value = EntryMessage.SaveFailed(e.localizedMessage)
            } finally {
                isSaving.value = false
            }
        }
    }

    companion object {
        fun factory(repository: BusinessEntryRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { BusinessEntryViewModel(repository) }
            }
    }
}
