package com.leen.bizentry.ui

import com.leen.bizentry.data.BusinessEntry
import com.leen.bizentry.data.BusinessEntryRepository
import com.leen.bizentry.data.CategoryTotal
import com.leen.bizentry.data.Settlement
import com.leen.bizentry.data.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    private val today = LocalDate.of(2026, 10, 22)

    private lateinit var repository: FakeBusinessEntryRepository
    private lateinit var viewModel: BusinessEntryViewModel

    @Before
    fun setUp() {
        repository = FakeBusinessEntryRepository()
        viewModel = BusinessEntryViewModel(
            repository,
            Clock.fixed(Instant.parse("2026-10-22T09:00:00Z"), ZoneId.of("Asia/Dhaka"))
        )
    }

    @Test
    fun nothingIsStoredUntilTheReadingIsConfirmed() = runTest {
        val states = collectStates()

        viewModel.onInputChange("আজ ৫০০০ টাকার কাপড় কিনেছি")
        viewModel.send()
        advanceUntilIdle()

        assertTrue("a draft must never be stored on its own", repository.stored().isEmpty())
        assertEquals(500_000L, states.last().draft?.amountMinor)
    }

    @Test
    fun confirmingStoresEverythingTheSentenceWasReadFor() = runTest {
        val states = collectStates()

        viewModel.onInputChange("আজ ৫০০০ টাকার কাপড় কিনেছি")
        viewModel.send()
        viewModel.confirmDraft()
        advanceUntilIdle()

        val stored = repository.stored().single()
        assertEquals("আজ ৫০০০ টাকার কাপড় কিনেছি", stored.rawText)
        assertEquals(500_000L, stored.amountMinor)
        assertEquals(TransactionType.PURCHASE, stored.type)
        assertEquals("কাপড়", stored.category)
        assertEquals(today.toEpochDay(), stored.transactionDateEpochDay!!)
        assertEquals("Asia/Dhaka", stored.transactionDateZone)
        assertEquals("BDT", stored.currency)
        assertEquals(EntryMessage.Saved, states.last().message)
        assertEquals(null, states.last().draft)
        assertEquals("", states.last().input)
    }

    @Test
    fun anUnreadableAmountIsAskedForAndThenAccepted() = runTest {
        val states = collectStates()

        viewModel.onInputChange("চাল কিনেছি")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(EntryMessage.AmountNotFound, states.last().message)
        assertEquals(null, states.last().draft?.amountMinor)

        viewModel.onInputChange("৫০০")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(50_000L, states.last().draft?.amountMinor)
        assertEquals(null, states.last().message)
    }

    @Test
    fun theAnsweredAmountIsFoldedIntoTheStoredSentence() = runTest {
        collectStates()

        viewModel.onInputChange("চাল কিনেছি")
        viewModel.send()
        viewModel.onInputChange("৫০০")
        viewModel.send()
        viewModel.confirmDraft()
        advanceUntilIdle()

        assertEquals("চাল কিনেছি ৫০০", repository.stored().single().rawText)
    }

    @Test
    fun anAnswerThatIsNotAnAmountIsRefused() = runTest {
        val states = collectStates()

        viewModel.onInputChange("চাল কিনেছি")
        viewModel.send()
        advanceUntilIdle()
        viewModel.onInputChange("জানি না")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(EntryMessage.AmountInvalid, states.last().message)
    }

    @Test
    fun aDraftWithNoAmountCannotBeConfirmed() = runTest {
        val states = collectStates()

        viewModel.onInputChange("চাল কিনেছি")
        viewModel.send()
        advanceUntilIdle()
        viewModel.confirmDraft()
        advanceUntilIdle()

        assertTrue(repository.stored().isEmpty())
        assertEquals(EntryMessage.AmountNotFound, states.last().message)
    }

    @Test
    fun cancellingThrowsTheDraftAway() = runTest {
        val states = collectStates()

        viewModel.onInputChange("আজ ৫০০ টাকা খরচ")
        viewModel.send()
        viewModel.cancelDraft()
        advanceUntilIdle()

        assertTrue(repository.stored().isEmpty())
        assertEquals(null, states.last().draft)
        assertEquals("", states.last().input)
    }

    @Test
    fun sendingNothingIsReportedAndInventsNoDraft() = runTest {
        val states = collectStates()

        viewModel.onInputChange("   ")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(EntryMessage.EmptyInput, states.last().message)
        assertEquals(null, states.last().draft)
    }

    @Test
    fun aFailedSaveKeepsTheDraftSoNothingIsLost() = runTest {
        repository.failNextAdd = true
        val states = collectStates()

        viewModel.onInputChange("আজ ৫০০ টাকা খরচ")
        viewModel.send()
        viewModel.confirmDraft()
        advanceUntilIdle()

        assertTrue(states.last().message is EntryMessage.SaveFailed)
        assertEquals(50_000L, states.last().draft?.amountMinor)
    }

    @Test
    fun editingReadsTheStoredEntryBackIntoTheComposer() = runTest {
        repository.seed(
            BusinessEntry(
                id = 7,
                rawText = "sold cloth",
                amountMinor = 500_000L,
                type = TransactionType.SALE,
                transactionDateEpochDay = 20_000L
            )
        )
        val states = collectStates()
        advanceUntilIdle()

        viewModel.startEditing(repository.stored().first())
        advanceUntilIdle()

        assertEquals(7L, states.last().editingId)
        assertEquals(500_000L, states.last().draft?.amountMinor)
        assertEquals("sold cloth", states.last().input)
    }

    @Test
    fun confirmingAnEditUpdatesInPlaceInsteadOfAddingARow() = runTest {
        repository.seed(BusinessEntry(id = 7, rawText = "sold cloth", amountMinor = 500_000L))
        val states = collectStates()
        advanceUntilIdle()

        viewModel.startEditing(repository.stored().first())
        viewModel.onInputChange("আজ ৭০০ টাকা কাপড় বিক্রি")
        viewModel.send()
        viewModel.confirmDraft()
        advanceUntilIdle()

        assertEquals(1, repository.stored().size)
        val stored = repository.stored().single()
        assertEquals(70_000L, stored.amountMinor)
        assertEquals(TransactionType.SALE, stored.type)
        assertEquals(EntryMessage.Updated, states.last().message)
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
    fun entriesReadOldestFirstSoTheTranscriptFlowsLikeAChat() = runTest {
        repository.seed(BusinessEntry(id = 1, rawText = "older", createdAt = 1))
        repository.seed(BusinessEntry(id = 2, rawText = "newer", createdAt = 2))
        val states = collectStates()
        advanceUntilIdle()

        assertEquals(listOf("older", "newer"), states.last().entries.map { it.rawText })
    }

    @Test
    fun theTypeCanBeCorrectedBeforeConfirming() = runTest {
        val states = collectStates()

        viewModel.onInputChange("আজ ৫০০ টাকা")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(TransactionType.UNKNOWN, states.last().draft?.type)

        viewModel.onDraftTypeChange(TransactionType.INCOME)
        viewModel.confirmDraft()
        advanceUntilIdle()

        assertEquals(TransactionType.INCOME, repository.stored().single().type)
    }

    @Test
    fun theCategoryCanBeCorrectedBeforeConfirming() = runTest {
        collectStates()

        viewModel.onInputChange("আজ ৫০০ টাকা")
        viewModel.send()
        viewModel.onDraftCategoryChange("বেতন")
        viewModel.confirmDraft()
        advanceUntilIdle()

        assertEquals("বেতন", repository.stored().single().category)
    }

    @Test
    fun correctingADraftLeavesTheOriginalSentenceAlone() = runTest {
        val states = collectStates()

        viewModel.onInputChange("আজ ৫০০ টাকা")
        viewModel.send()
        viewModel.onDraftTypeChange(TransactionType.INCOME)
        advanceUntilIdle()

        assertEquals("আজ ৫০০ টাকা", states.last().draft?.rawText)
    }

    @Test
    fun cashOrCreditIsStoredFromTheSentence() = runTest {
        collectStates()

        viewModel.onInputChange("বাকিতে ৫০০০ টাকার কাপড় বিক্রি")
        viewModel.send()
        viewModel.confirmDraft()
        advanceUntilIdle()

        assertEquals(Settlement.CREDIT, repository.stored().single().settlement)
    }

    @Test
    fun cashOrCreditCanBeCorrectedBeforeConfirming() = runTest {
        collectStates()

        viewModel.onInputChange("৫০০ টাকা বিক্রি")
        viewModel.send()
        viewModel.onDraftSettlementChange(Settlement.CASH)
        viewModel.confirmDraft()
        advanceUntilIdle()

        assertEquals(Settlement.CASH, repository.stored().single().settlement)
    }

    @Test
    fun aFailureIsReportedByItsShapeNotByItsRawText() = runTest {
        repository.failNextAdd = true
        val states = collectStates()

        viewModel.onInputChange("500 taka")
        viewModel.send()
        viewModel.confirmDraft()
        advanceUntilIdle()

        val message = states.last().message
        assertTrue(message is EntryMessage.SaveFailed)
        val detail = (message as EntryMessage.SaveFailed).detail
        assertEquals("IllegalStateException", detail)
        assertFalse(
            "raw exception text must not reach the screen",
            detail?.contains("storage unavailable") ?: false
        )
    }
    private fun TestScope.collectStates(): List<BusinessEntryUiState> {
        val states = mutableListOf<BusinessEntryUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states += it }
        }
        return states
    }
}

private class FakeBusinessEntryRepository : BusinessEntryRepository {

    private val entries = MutableStateFlow<List<BusinessEntry>>(emptyList())

    var failNextAdd = false

    override val allEntries: Flow<List<BusinessEntry>> =
        entries.map { stored -> stored.sortedByDescending { it.createdAt } }

    override suspend fun add(entry: BusinessEntry): Long {
        if (failNextAdd) {
            failNextAdd = false
            throw IllegalStateException("storage unavailable")
        }
        val stored = entry.copy(id = (entries.value.maxOfOrNull { it.id } ?: 0L) + 1L)
        entries.value = entries.value + stored
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

    fun stored(): List<BusinessEntry> = entries.value

    override suspend fun sumInRange(fromEpochDay: Long, toEpochDay: Long): Long? = null

    override suspend fun sumByType(
        type: TransactionType,
        fromEpochDay: Long,
        toEpochDay: Long
    ): Long? = null

    override suspend fun sumBySettlement(
        settlement: Settlement,
        fromEpochDay: Long,
        toEpochDay: Long
    ): Long? = null

    override suspend fun totalsByCategory(
        fromEpochDay: Long,
        toEpochDay: Long
    ): List<CategoryTotal> = emptyList()
}
