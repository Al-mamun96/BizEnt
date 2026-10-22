package com.leen.bizentry.parse

import org.junit.Assert.assertEquals
import org.junit.Test

class BengaliTextTest {

    private val bengaliDigits =
        "\u09E6\u09E7\u09E8\u09E9\u09EA\u09EB\u09EC\u09ED\u09EE\u09EF"

    @Test
    fun convertsBengaliDigitsToAscii() {
        assertEquals("0123456789", BengaliText.toAsciiDigits(bengaliDigits))
    }

    @Test
    fun leavesAsciiAndLettersUntouched() {
        assertEquals("500 taka", BengaliText.toAsciiDigits("500 taka"))
    }

    @Test
    fun handlesBengaliAndAsciiDigitsInTheSameNumber() {
        assertEquals("50", BengaliText.toAsciiDigits("\u09EB0"))
    }

    @Test
    fun bothSpellingsOfYyaCanonicaliseToTheSameSequence() {
        assertEquals(
            BengaliText.canonicalize("\u09DF"),
            BengaliText.canonicalize("\u09AF\u09BC")
        )
    }

    @Test
    fun bothSpellingsOfRraCanonicaliseToTheSameSequence() {
        assertEquals(
            BengaliText.canonicalize("\u09DC"),
            BengaliText.canonicalize("\u09A1\u09BC")
        )
    }
}
