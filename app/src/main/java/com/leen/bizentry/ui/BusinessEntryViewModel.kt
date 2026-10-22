package com.leen.bizentry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leen.bizentry.data.BusinessEntry
import com.leen.bizentry.data.BusinessEntryRepository
import com.leen.bizentry.data.DEFAULT_CATEGORY
import com.leen.bizentry.data.Money
import com.leen.bizentry.data.Settlement
import com.leen.bizentry.data.TransactionType
import com.leen.bizentry.parse.EntryParser
import com.leen.bizentry.parse.ParsedEntry
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
    data object AmountNotFound : EntryMessage
    data object AmountInvalid : EntryMessage
    data object Saved : EntryMessage
    data object Updated : EntryMessage
    data object Deleted : EntryMessage
    data class SaveFailed(val detail: String?) : EntryMessage
}

data class BusinessEntryUiState(
    val entries: List<BusinessEntry> = emptyList(),
    val input: String = "",
    val draft: ParsedEntry? = null,
    val editingId: Long? = null,
    val isSaving: Boolean = false,
    val message: EntryMessage? = null
)

private data class Composer(
    val input: String = "",
    val draft: ParsedEntry? = null,
    val editing: BusinessEntry? = null
)

/**
 * The entry screen is a conversation: you type a sentence, [EntryParser] reads what it can, and the
 * reading is only stored once you confirm it.
 *
 * A draft whose amount could not be read is not confirmable, so the next message is taken as the
 * answer to "how much?". That fallback is what keeps a rule parser usable before it is good at
 * every phrasing, and it is also what makes the local model optional later.
 */
class BusinessEntryViewModel(
    private val repository: BusinessEntryRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val composer = MutableStateFlow(Composer())
    private val isSaving = MutableStateFlow(false)
    private val message = MutableStateFlow<EntryMessage?>(null)

    val uiState: StateFlow<BusinessEntryUiState> = combine(
        repository.allEntries,
        composer,
        isSaving,
        message
    ) { entries, currentComposer, saving, currentMessage ->
        BusinessEntryUiState(
            entries = entries.reversed(),
            input = currentComposer.input,
            draft = currentComposer.draft,
            editingId = currentComposer.editing?.id,
            isSaving = saving,
            message = currentMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BusinessEntryUiState()
    )

    fun onInputChange(value: String) {
        composer.value = composer.value.copy(input = value)
        message.value = null
    }

    fun send() {
        val sent = composer.value.input.trim()
        if (sent.isEmpty()) {
            message.value = EntryMessage.EmptyInput
            return
        }

        val openDraft = composer.value.draft

        if (openDraft != null && openDraft.amountMinor == null) {
            acceptAmount(sent, openDraft)
            return
        }

        val parsed = EntryParser.parse(sent, today())
        composer.value = composer.value.copy(input = "", draft = parsed)
        message.value = if (parsed.amountMinor == null) EntryMessage.AmountNotFound else null
    }

    fun confirmDraft() {
        val current = composer.value
        val draft = current.draft ?: return

        if (draft.amountMinor == null) {
            message.value = EntryMessage.AmountNotFound
            return
        }

        if (isSaving.value) return
        isSaving.value = true

        val existing = current.editing
        val date = draft.transactionDate ?: today()

        viewModelScope.launch {
            try {
                if (existing != null) {
                    repository.update(
                        existing.copy(
                            rawText = draft.rawText,
                            amountMinor = draft.amountMinor,
                            currency = existing.currency ?: DEFAULT_CURRENCY,
                            type = draft.type,
                            settlement = draft.settlement,
                            category = draft.category ?: existing.category,
                            transactionDateEpochDay = date.toEpochDay(),
                            transactionDateZone = clock.zone.id,
                            updatedAt = clock.millis()
                        )
                    )
                    message.value = EntryMessage.Updated
                } else {
                    repository.add(
                        BusinessEntry(
                            rawText = draft.rawText,
                            amountMinor = draft.amountMinor,
                            currency = DEFAULT_CURRENCY,
                            type = draft.type,
                            settlement = draft.settlement,
                            category = draft.category ?: DEFAULT_CATEGORY,
                            transactionDateEpochDay = date.toEpochDay(),
                            transactionDateZone = clock.zone.id,
                            createdAt = clock.millis()
                        )
                    )
                    message.value = EntryMessage.Saved
                }

                composer.value = Composer()
            } catch (e: Exception) {
                message.value = EntryMessage.SaveFailed(e::class.simpleName)
            } finally {
                isSaving.value = false
            }
        }
    }

    fun cancelDraft() {
        composer.value = Composer()
        message.value = null
    }

    fun onDraftTypeChange(type: TransactionType) {
        val draft = composer.value.draft ?: return
        composer.value = composer.value.copy(draft = draft.copy(type = type))
        message.value = null
    }

    fun onDraftCategoryChange(category: String) {
        val draft = composer.value.draft ?: return
        composer.value = composer.value.copy(draft = draft.copy(category = category))
        message.value = null
    }

    fun onDraftSettlementChange(settlement: Settlement) {
        val draft = composer.value.draft ?: return
        composer.value = composer.value.copy(draft = draft.copy(settlement = settlement))
        message.value = null
    }

    fun startEditing(entry: BusinessEntry) {
        composer.value = Composer(
            input = entry.rawText,
            draft = ParsedEntry(
                rawText = entry.rawText,
                amountMinor = entry.amountMinor,
                type = entry.type,
                settlement = entry.settlement,
                transactionDate = entry.transactionDateEpochDay?.let { LocalDate.ofEpochDay(it) },
                category = entry.category
            ),
            editing = entry
        )
        message.value = null
    }

    fun delete(entry: BusinessEntry) {
        if (isSaving.value) return
        isSaving.value = true

        viewModelScope.launch {
            try {
                repository.delete(entry)
                if (composer.value.editing?.id == entry.id) {
                    composer.value = Composer()
                }
                message.value = EntryMessage.Deleted
            } catch (e: Exception) {
                message.value = EntryMessage.SaveFailed(e::class.simpleName)
            } finally {
                isSaving.value = false
            }
        }
    }

    private fun acceptAmount(answer: String, draft: ParsedEntry) {
        val amountMinor = EntryParser.parse(answer, today()).amountMinor
            ?: Money.parseMinorUnits(answer)

        if (amountMinor == null) {
            message.value = EntryMessage.AmountInvalid
            return
        }

        composer.value = composer.value.copy(
            input = "",
            draft = draft.copy(
                rawText = "${draft.rawText} $answer",
                amountMinor = amountMinor
            )
        )
        message.value = null
    }

    private fun today(): LocalDate = LocalDate.now(clock)

    companion object {
        fun factory(repository: BusinessEntryRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { BusinessEntryViewModel(repository) }
            }
    }
}
