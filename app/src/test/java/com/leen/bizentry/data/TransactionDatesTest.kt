package com.leen.bizentry.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TransactionDatesTest {

    @Test
    fun aDateSurvivesThePickerRoundTrip() {
        val dates = listOf(
            LocalDate.of(1970, 1, 1),
            LocalDate.of(2000, 2, 29),
            LocalDate.of(2026, 10, 22),
            LocalDate.of(2099, 12, 31)
        )

        dates.forEach { date ->
            assertEquals(date, TransactionDates.fromPickerMillis(TransactionDates.toPickerMillis(date)))
        }
    }

    @Test
    fun pickerMillisAreMidnightUtcSoTheChosenDayNeverSlides() {
        assertEquals(0L, TransactionDates.toPickerMillis(LocalDate.of(1970, 1, 1)))
        assertEquals(
            LocalDate.of(2026, 10, 22).toEpochDay() * 86_400_000L,
            TransactionDates.toPickerMillis(LocalDate.of(2026, 10, 22))
        )
    }
}
