package com.leen.bizentry.ui

import com.leen.bizentry.data.BusinessEntry
import com.leen.bizentry.data.BusinessEntryRepository
import com.leen.bizentry.data.CategoryTotal
import com.leen.bizentry.data.EpochDayRange
import com.leen.bizentry.data.ReportPeriod
import com.leen.bizentry.data.Settlement
import com.leen.bizentry.data.TransactionType
import com.leen.bizentry.data.rangeIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 22)
    private val monday = DayOfWeek.MONDAY

    private val clock = Clock.fixed(Instant.parse("2026-10-22T09:00:00Z"), ZoneId.of("Asia/Dhaka"))

    private fun viewModel(repository: BusinessEntryRepository) =
        ReportViewModel(repository, clock, monday)

    @Test
    fun itStartsOnThisMonthUsingTheRangeArithmetic() = runTest {
        val model = viewModel(StubRepository())

        advanceUntilIdle()

        assertEquals(ReportPeriod.THIS_MONTH, model.summary.value.period)
        assertEquals(ReportPeriod.THIS_MONTH.rangeIn(today, monday), model.summary.value.range)
    }

    @Test
    fun toTheRangeThatWasAskedFor() = runTest {
        val model = viewModel(StubRepository(total = 500_000L, cash = 200_000L, credit = 100_000L))

        model.select(ReportPeriod.TODAY)
        advanceUntilIdle()

        assertEquals(ReportPeriod.TODAY.rangeIn(today, monday), model.summary.value.range)
        assertEquals(500_000L, model.summary.value.totalMinor)
        assertEquals(200_000L, model.summary.value.cashMinor)
        assertEquals(100_000L, model.summary.value.creditMinor)
    }

    @Test
    fun entriesWithNoSettlementStatedAreVisibleAsTheirOwnFigure() = runTest {
        val model = viewModel(StubRepository(total = 500_000L, cash = 200_000L, credit = 100_000L))

        advanceUntilIdle()

        assertEquals(200_000L, model.summary.value.notStatedMinor)
    }

    @Test
    fun anEmptyPeriodReportsZeroesRatherThanNothing() = runTest {
        val model = viewModel(StubRepository())

        model.select(ReportPeriod.LAST_MONTH)
        advanceUntilIdle()

        val summary = model.summary.value
        assertEquals(0L, summary.totalMinor)
        assertEquals(0L, summary.cashMinor)
        assertEquals(0L, summary.creditMinor)
        assertEquals(0L, summary.notStatedMinor)
        assertEquals(TransactionType.entries.count { it != TransactionType.UNKNOWN }, summary.byType.size)
    }

    @Test
    fun categoriesAreHandedStraightThrough() = runTest {
        val categories = listOf(CategoryTotal("কাপড়", 500_000L), CategoryTotal("খাবার", 300_000L))
        val model = viewModel(StubRepository(categories = categories))

        advanceUntilIdle()

        assertEquals(categories, model.summary.value.byCategory)
    }
}

private class StubRepository(
    private val total: Long? = null,
    private val cash: Long? = null,
    private val credit: Long? = null,
    private val categories: List<CategoryTotal> = emptyList()
) : BusinessEntryRepository {

    override val allEntries: Flow<List<BusinessEntry>> = MutableStateFlow(emptyList())

    override suspend fun add(entry: BusinessEntry): Long = 0L

    override suspend fun update(entry: BusinessEntry) = Unit

    override suspend fun delete(entry: BusinessEntry) = Unit

    override suspend fun findPossibleDuplicates(
        rawText: String,
        sinceMillis: Long
    ): List<BusinessEntry> = emptyList()

    override suspend fun sumInRange(fromEpochDay: Long, toEpochDay: Long): Long? = total

    override suspend fun sumByType(
        type: TransactionType,
        fromEpochDay: Long,
        toEpochDay: Long
    ): Long? = null

    override suspend fun sumBySettlement(
        settlement: Settlement,
        fromEpochDay: Long,
        toEpochDay: Long
    ): Long? = if (settlement == Settlement.CASH) cash else credit

    override suspend fun totalsByCategory(
        fromEpochDay: Long,
        toEpochDay: Long
    ): List<CategoryTotal> = categories
}
