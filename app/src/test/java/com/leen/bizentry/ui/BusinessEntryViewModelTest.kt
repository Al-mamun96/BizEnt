package com.leen.bizentry.ui

import com.leen.bizentry.data.BusinessEntry
import com.leen.bizentry.data.BusinessEntryRepository
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

    override suspend fun update(entry: BusinessEntry) = Unit

    override suspend fun delete(entry: BusinessEntry) = Unit

    override suspend fun findPossibleDuplicates(
        rawText: String,
        sinceMillis: Long
    ): List<BusinessEntry> = emptyList()

    fun seed(entry: BusinessEntry) {
        entries.value = entries.value + entry
    }

    fun storedTexts(): List<String> = entries.value.map { it.rawText }
}
