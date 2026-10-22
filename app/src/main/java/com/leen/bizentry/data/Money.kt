package com.leen.bizentry.data

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Money is held as minor units (poisha) in a Long, never as a floating point type, so that values
 * stay exact. Parsing is deliberately strict: anything ambiguous is rejected rather than guessed,
 * and more than two decimal places is a rejection instead of a silent rounding.
 */
object Money {

    private val AMOUNT_PATTERN = Regex("""\d{1,15}(\.\d{1,2})?""")

    fun parseMinorUnits(input: String): Long? {
        val trimmed = input.trim()
        if (!AMOUNT_PATTERN.matches(trimmed)) return null

        val minorUnits = BigDecimal(trimmed)
            .setScale(2, RoundingMode.UNNECESSARY)
            .movePointRight(2)
            .longValueExact()

        return if (minorUnits > 0L) minorUnits else null
    }

    fun format(minorUnits: Long): String =
        BigDecimal.valueOf(minorUnits).movePointLeft(2).setScale(2).toPlainString()
}
