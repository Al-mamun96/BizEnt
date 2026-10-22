package com.leen.bizentry.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun parsesWholeAmounts() {
        assertEquals(500_000L, Money.parseMinorUnits("5000"))
    }

    @Test
    fun parsesTwoDecimalPlaces() {
        assertEquals(500_050L, Money.parseMinorUnits("5000.50"))
    }

    @Test
    fun parsesASingleDecimalPlace() {
        assertEquals(500_050L, Money.parseMinorUnits("5000.5"))
    }

    @Test
    fun ignoresSurroundingWhitespace() {
        assertEquals(100L, Money.parseMinorUnits("  1  "))
    }

    @Test
    fun rejectsMoreThanTwoDecimalsInsteadOfSilentlyRounding() {
        assertNull(Money.parseMinorUnits("12.345"))
    }

    @Test
    fun rejectsZeroAndNegativeAmounts() {
        assertNull(Money.parseMinorUnits("0"))
        assertNull(Money.parseMinorUnits("0.00"))
        assertNull(Money.parseMinorUnits("-5"))
    }

    @Test
    fun rejectsAnythingThatIsNotAPlainAmount() {
        val rejected = listOf(
            "", "   ", "abc", "12abc", "5 000", "+5", "1e3", ".5", "5.", "1,000", "৳500"
        )

        rejected.forEach { input ->
            assertNull("'$input' must be rejected rather than guessed", Money.parseMinorUnits(input))
        }
    }

    @Test
    fun formatsMinorUnitsWithTwoDecimalPlaces() {
        assertEquals("5.00", Money.format(500L))
        assertEquals("5000.00", Money.format(500_000L))
        assertEquals("5000.50", Money.format(500_050L))
        assertEquals("0.05", Money.format(5L))
    }

    @Test
    fun minorUnitsSurviveAFormatThenParseRoundTrip() {
        val values = listOf(1L, 5L, 100L, 500_050L, 999_999_999_999L)

        values.forEach { minorUnits ->
            assertEquals(minorUnits, Money.parseMinorUnits(Money.format(minorUnits)))
        }
    }
}
