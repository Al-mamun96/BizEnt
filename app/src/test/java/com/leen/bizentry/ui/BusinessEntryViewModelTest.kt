package com.leen.bizentry.ui

import com.leen.bizentry.data.BusinessEntry
import com.leen.bizentry.data.BusinessEntryRepository
import com.leen.bizentry.data.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class BusinessEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeBusinessEntryRepository
    private lateinit var viewModel: BusinessEntryViewModel

    @Before
    fun setUp() {
        repository = FakeBusinessEntryRepository()
        viewModel = BusinessEntryViewModel(repository)
    }

    @Test
    fun savingStoresTheTrimmedTextAndClearsTheInput() = runTest {
        val states = collectStates()

        viewModel.onTextChange("  paid rent  ")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf("paid rent"), repository.storedTexts())
        assertEquals("", states.last().text)
        assertEquals(EntryMessage.Saved, states.last().message)
        assertFalse(states.last().isSaving)
    }

    @Test
    fun savingBlankInputReportsEmptyInputAndStoresNothing() = runTest {
        val states = collectStates()

        viewModel.onTextChange("   ")
        viewModel.save()
        advanceUntilIdle()

        assertTrue(repository.storedTexts().isEmpty())
        assertEquals(EntryMessage.EmptyInput, states.last().message)
    }

    @Test
    fun aFailedSaveKeepsTheTextSoNothingIsLost() = runTest {
        repository.failNextAdd = true
        val states = collectStates()

        viewModel.onTextChange("paid rent")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("paid rent", states.last().text)
        assertTrue(states.last().message is EntryMessage.SaveFailed)
        assertFalse(states.last().isSaving)
    }

    @Test
    fun storedEntriesReachTheUiState() = runTest {
        repository.seed(BusinessEntry(rawText = "existing entry", createdAt = 1))
        val states = collectStates()
        advanceUntilIdle()

        assertEquals(listOf("existing entry"), states.last().entries.map { it.rawText })
    }

    @Test
    fun typingClearsAPreviousMessage() = runTest {
        val states = collectStates()

        viewModel.onTextChange("   ")
        viewModel.save()
        advanceUntilIdle()
        viewModel.onTextChange("now something")
        advanceUntilIdle()

        assertEquals(null, states.last().message)
    }

    @Test
    fun anUnparseableAmountBlocksTheSaveAndReportsIt() = runTest {
        val states = collectStates()

        viewModel.onTextChange("paid rent")
        viewModel.onAmountChange("12.345")
        viewModel.save()
        advanceUntilIdle()

        assertTrue(repository.storedTexts().isEmpty())
        assertEquals(EntryMessage.AmountInvalid, states.last().message)
    }

    @Test
    fun aValidAmountIsStoredAsMinorUnitsAndClearsTheAmountField() = runTest {
        val states = collectStates()

        viewModel.onTextChange("paid rent")
        viewModel.onAmountChange("5000.50")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf(500_050L), repository.storedAmounts())
        assertEquals("", states.last().amountInput)
    }

    @Test
    fun anEntryWithNoAmountKeepsANullAmountAndCurrency() = runTest {
        collectStates()

        viewModel.onTextChange("just a note")
        viewModel.save()
        advanceUntilIdle()

        val stored = repository.storedEntry()!!
        assertEquals(null, stored.amountMinor)
        assertEquals(null, stored.currency)
    }

    @Test
    fun theDateDefaultsToTodayInTheDeviceZone() = runTest {
        val dhaka = ZoneId.of("Asia/Dhaka")
        val fixedClock = Clock.fixed(Instant.parse("2026-10-21T20:00:00Z"), dhaka)
        val dated = BusinessEntryViewModel(repository, fixedClock)
        val states = collectStatesFor(dated)

        advanceUntilIdle()

        assertEquals(
            LocalDate.of(2026, 10, 22).toEpochDay(),
            states.last().transactionDateEpochDay
        )
    }

    @Test
    fun theChosenDateZoneAndTimestampAreStoredWithTheEntry() = runTest {
        val dhaka = ZoneId.of("Asia/Dhaka")
        val fixedClock = Clock.fixed(Instant.parse("2026-10-22T09:00:00Z"), dhaka)
        val dated = BusinessEntryViewModel(repository, fixedClock)
        collectStatesFor(dated)

        dated.onTextChange("paid rent")
        dated.onDateChange(LocalDate.of(2026, 10, 20))
        dated.save()
        advanceUntilIdle()

        val stored = repository.storedEntry()!!
        assertEquals(LocalDate.of(2026, 10, 20).toEpochDay(), stored.transactionDateEpochDay!!)
        assertEquals("Asia/Dhaka", stored.transactionDateZone)
        assertEquals(fixedClock.millis(), stored.createdAt)
    }

    @Test
    fun theChosenTypeIsStoredWithTheEntry() = runTest {
        collectStates()

        viewModel.onTextChange("sold cloth")
        viewModel.onTypeChange(TransactionType.SALE)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(TransactionType.SALE, repository.storedEntry()!!.type)
    }

    @Test
    fun theTypeStartsOnExpenseAndKeepsTheChosenValueAfterSaving() = runTest {
        val states = collectStates()
        assertEquals(TransactionType.EXPENSE, states.last().type)

        viewModel.onTextChange("consulting fee")
        viewModel.onTypeChange(TransactionType.INCOME)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(TransactionType.INCOME, states.last().type)
    }

    @Test
    fun editingLoadsTheEntryIntoTheForm() = runTest {
        repository.seed(
            BusinessEntry(
                id = 7,
                rawText = "sold cloth",
                amountMinor = 500_050L,
                type = TransactionType.SALE,
                transactionDateEpochDay = 20_000L
            )
        )
        val states = collectStates()
        advanceUntilIdle()

        viewModel.startEditing(repository.stored().first())
        advanceUntilIdle()

        val last = states.last()
        assertEquals("sold cloth", last.text)
        assertEquals("5000.50", last.amountInput)
        assertEquals(TransactionType.SALE, last.type)
        assertEquals(20_000L, last.transactionDateEpochDay)
        assertEquals(7L, last.editingId)
    }

    @Test
    fun savingWhileEditingUpdatesInPlaceInsteadOfAddingARow() = runTest {
        repository.seed(BusinessEntry(id = 7, rawText = "sold cloth", amountMinor = 500_000L))
        val states = collectStates()
        advanceUntilIdle()

        viewModel.startEditing(repository.stored().first())
        viewModel.onTextChange("sold cloth corrected")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, repository.stored().size)
        assertEquals("sold cloth corrected", repository.stored().single().rawText)
        assertTrue(repository.stored().single().updatedAt != null)
        assertEquals(EntryMessage.Updated, states.last().message)
        assertEquals(null, states.last().editingId)
    }

    @Test
    fun cancellingAnEditLeavesTheStoredEntryAlone() = runTest {
        repository.seed(BusinessEntry(id = 7, rawText = "sold cloth"))
        val states = collectStates()
        advanceUntilIdle()

        viewModel.startEditing(repository.stored().first())
        viewModel.onTextChange("half typed")
        viewModel.cancelEditing()
        advanceUntilIdle()

        assertEquals("sold cloth", repository.stored().single().rawText)
        assertEquals("", states.last().text)
        assertEquals(null, states.last().editingId)
    }

    @Test
    fun editingALegacyEntryWithNoDateFallsBackToToday() = runTest {
        val dhaka = ZoneId.of("Asia/Dhaka")
        val fixedClock = Clock.fixed(Instant.parse("2026-10-22T09:00:00Z"), dhaka)
        val dated = BusinessEntryViewModel(repository, fixedClock)
        repository.seed(BusinessEntry(id = 7, rawText = "legacy row"))
        val states = collectStatesFor(dated)
        advanceUntilIdle()

        dated.startEditing(repository.stored().first())
        advanceUntilIdle()

        assertEquals(
            LocalDate.of(2026, 10, 22).toEpochDay(),
            states.last().transactionDateEpochDay
        )
    }

    @Test
    fun deletingRemovesTheEntry() = runTest {
        repository.seed(BusinessEntry(id = 7, rawText = "sold cloth"))
        val states = collectStates()
        advanceUntilIdle()

        viewModel.delete(repository.stored().first())
        advanceUntilIdle()

        assertTrue(repository.stored().isEmpty())
        assertEquals(EntryMessage.Deleted, states.last().message)
    }

    @Test
    fun deletingTheEntryBeingEditedAlsoLeavesEditMode() = runTest {
        repository.seed(BusinessEntry(id = 7, rawText = "sold cloth"))
        val states = collectStates()
        advanceUntilIdle()

        viewModel.startEditing(repository.stored().first())
        viewModel.delete(repository.stored().first())
        advanceUntilIdle()

        assertEquals(null, states.last().editingId)
        assertEquals("", states.last().text)
    }

    private fun TestScope.collectStates(): List<BusinessEntryUiState> =
        collectStatesFor(viewModel)

    private fun TestScope.collectStatesFor(
        target: BusinessEntryViewModel
    ): List<BusinessEntryUiState> {
        val states = mutableListOf<BusinessEntryUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            target.uiState.collect { states += it }
        }
        return states
    }
}

private class FakeBusinessEntryRepository : BusinessEntryRepository {

    private val entries = MutableStateFlow<List<BusinessEntry>>(emptyList())

    var failNextAdd = false

    override val allEntries: Flow<List<BusinessEntry>> = entries

    override suspend fun add(entry: BusinessEntry): Long {
        if (failNextAdd) {
            failNextAdd = false
            throw IllegalStateException("storage unavailable")
        }
        val stored = entry.copy(id = (entries.value.maxOfOrNull { it.id } ?: 0L) + 1L)
        entries.value = listOf(stored) + entries.value
        return stored.id
    }

    override suspend fun update(entry: BusinessEntry) {
        entries.value = entries.value.map { if (it.id == entry.id) entry else it }
    }

    override suspend fun delete(entry: BusinessEntry) {
        entries.value = entries.value.filterNot { it.id == entry.id }
    }

    override suspend fun findPossibleDuplicates(
        rawText: String,
        sinceMillis: Long
    ): List<BusinessEntry> = emptyList()

    fun seed(entry: BusinessEntry) {
        entries.value = entries.value + entry
    }

    fun storedTexts(): List<String> = entries.value.map { it.rawText }

    fun storedAmounts(): List<Long?> = entries.value.map { it.amountMinor }

    fun storedEntry(): BusinessEntry? = entries.value.firstOrNull()

    fun stored(): List<BusinessEntry> = entries.value
}
