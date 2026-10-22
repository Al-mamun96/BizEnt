package com.leen.bizentry.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

data class EpochDayRange(val from: Long, val to: Long)

enum class ReportPeriod {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH
}

/**
 * Inclusive epoch-day bounds for a report period, ready to hand to the ranged queries.
 *
 * The first day of the week comes from the device locale rather than being assumed. ISO says
 * Monday, but a Sunday-start week would shift every weekly total by a day and quietly move
 * transactions between weeks, and Bangladesh does not necessarily follow ISO.
 */
fun ReportPeriod.rangeIn(
    today: LocalDate,
    firstDayOfWeek: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
): EpochDayRange = when (this) {
    ReportPeriod.TODAY -> EpochDayRange(today.toEpochDay(), today.toEpochDay())

    ReportPeriod.THIS_WEEK -> {
        val start = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        EpochDayRange(start.toEpochDay(), start.plusDays(6).toEpochDay())
    }

    ReportPeriod.THIS_MONTH -> EpochDayRange(
        today.withDayOfMonth(1).toEpochDay(),
        today.with(TemporalAdjusters.lastDayOfMonth()).toEpochDay()
    )

    ReportPeriod.LAST_MONTH -> {
        val previousMonth = today.minusMonths(1)
        EpochDayRange(
            previousMonth.withDayOfMonth(1).toEpochDay(),
            previousMonth.with(TemporalAdjusters.lastDayOfMonth()).toEpochDay()
        )
    }
}
