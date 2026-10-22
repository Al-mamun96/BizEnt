package com.leen.bizentry

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.leen.bizentry.data.BusinessEntry
import com.leen.bizentry.data.BusinessEntryRepository
import com.leen.bizentry.data.TransactionType
import com.leen.bizentry.ui.BusinessEntryViewModel
import com.leen.bizentry.ui.theme.BizEntryTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Drives the real screen, so the wiring that the ViewModel tests cannot see - the text field, the
 * send button, the draft bubble, the correction menus and the confirm button - is actually
 * exercised rather than merely compiled.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en")
class BusinessEntryScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val repository = ScreenFakeRepository()

    private val viewModel = BusinessEntryViewModel(
        repository,
        Clock.fixed(Instant.parse("2026-10-22T09:00:00Z"), ZoneId.of("Asia/Dhaka"))
    )

    private fun render() {
        compose.setContent {
            BizEntryTheme { BusinessEntryScreen(viewModel) }
        }
    }

    private fun send(text: String) {
        compose.onNode(hasSetTextAction()).performTextInput(text)
        compose.onNodeWithText("Send").performClick()
    }

    @Test
    fun anEmptyLedgerShowsTheEmptyState() {
        render()

        compose.onNodeWithText("No entries yet.").assertIsDisplayed()
    }

    @Test
    fun aSentenceBecomesADraftAndNothingIsStoredYet() {
        render()

        send("500 taka khoroch")

        compose.onNodeWithText("Amount: 500.00").assertIsDisplayed()
        compose.onNodeWithText("Type: Expense").assertIsDisplayed()
        assertTrue("a draft must not be stored before confirmation", repository.stored().isEmpty())
    }

    @Test
    fun confirmingTheDraftStoresEverythingItRead() {
        render()

        send("500 taka khoroch")
        compose.onNodeWithText("Save entry").performClick()
        compose.waitUntil(5_000) { repository.stored().isNotEmpty() }

        val stored = repository.stored().single()
        assertEquals(50_000L, stored.amountMinor)
        assertEquals(TransactionType.EXPENSE, stored.type)
        assertEquals(LocalDate.of(2026, 10, 22).toEpochDay(), stored.transactionDateEpochDay!!)
    }

    @Test
    fun theTypeCanBeCorrectedFromTheDraftBeforeSaving() {
        render()

        send("500 taka khoroch")
        compose.onNodeWithText("Type: Expense").performClick()
        compose.onNodeWithText("Income").performClick()
        compose.onNodeWithText("Save entry").performClick()
        compose.waitUntil(5_000) { repository.stored().isNotEmpty() }

        assertEquals(TransactionType.INCOME, repository.stored().single().type)
    }

    @Test
    fun cancellingTheDraftRemovesItAndStoresNothing() {
        render()

        send("500 taka khoroch")
        compose.onNodeWithText("Cancel").performClick()

        compose.onNodeWithText("Amount: 500.00").assertDoesNotExist()
        assertTrue(repository.stored().isEmpty())
    }
}

private class ScreenFakeRepository : BusinessEntryRepository {

    private val entries = MutableStateFlow<List<BusinessEntry>>(emptyList())

    override val allEntries: Flow<List<BusinessEntry>> =
        entries.map { stored -> stored.sortedByDescending { it.createdAt } }

    override suspend fun add(entry: BusinessEntry): Long {
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

    fun stored(): List<BusinessEntry> = entries.value
}
