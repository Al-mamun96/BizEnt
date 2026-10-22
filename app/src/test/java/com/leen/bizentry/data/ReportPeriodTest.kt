package com.leen.bizentry.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ReportPeriodTest {

    private val monday = DayOfWeek.MONDAY

    @Test
    fun todayIsASingleDay() {
        val today = LocalDate.of(2026, 10, 22)

        val range = ReportPeriod.TODAY.rangeIn(today, monday)

        assertEquals(today.toEpochDay(), range.from)
        assertEquals(today.toEpochDay(), range.to)
    }

    @Test
    fun thisWeekRunsFromTheFirstDayForSevenDays() {
        val thursday = LocalDate.of(2026, 10, 22)

        val range = ReportPeriod.THIS_WEEK.rangeIn(thursday, monday)

        assertEquals(LocalDate.of(2026, 10, 19).toEpochDay(), range.from)
        assertEquals(LocalDate.of(2026, 10, 25).toEpochDay(), range.to)
    }

    @Test
    fun aSundayWeekStartShiftsTheWholeRange() {
        val thursday = LocalDate.of(2026, 10, 22)

        val range = ReportPeriod.THIS_WEEK.rangeIn(thursday, DayOfWeek.SUNDAY)

        assertEquals(LocalDate.of(2026, 10, 18).toEpochDay(), range.from)
        assertEquals(LocalDate.of(2026, 10, 24).toEpochDay(), range.to)
    }

    @Test
    fun thisMonthCoversTheWholeMonthIncludingItsLastDay() {
        val range = ReportPeriod.THIS_MONTH.rangeIn(LocalDate.of(2026, 10, 22), monday)

        assertEquals(LocalDate.of(2026, 10, 1).toEpochDay(), range.from)
        assertEquals(LocalDate.of(2026, 10, 31).toEpochDay(), range.to)
    }

    @Test
    fun lastMonthKnowsHowLongThePreviousMonthWas() {
        val range = ReportPeriod.LAST_MONTH.rangeIn(LocalDate.of(2026, 3, 15), monday)

        assertEquals(LocalDate.of(2026, 2, 1).toEpochDay(), range.from)
        assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), range.to)
    }

    @Test
    fun lastMonthHandlesALeapFebruary() {
        val range = ReportPeriod.LAST_MONTH.rangeIn(LocalDate.of(2024, 3, 15), monday)

        assertEquals(LocalDate.of(2024, 2, 1).toEpochDay(), range.from)
        assertEquals(LocalDate.of(2024, 2, 29).toEpochDay(), range.to)
    }

    @Test
    fun lastMonthFromJanuaryCrossesIntoThePreviousYear() {
        val range = ReportPeriod.LAST_MONTH.rangeIn(LocalDate.of(2026, 1, 10), monday)

        assertEquals(LocalDate.of(2025, 12, 1).toEpochDay(), range.from)
        assertEquals(LocalDate.of(2025, 12, 31).toEpochDay(), range.to)
    }
}
