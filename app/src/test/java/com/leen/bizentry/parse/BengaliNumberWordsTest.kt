package com.leen.bizentry.parse

import org.junit.Assert.assertEquals
import org.junit.Test

class BengaliNumberWordsTest {

    @Test
    fun rewritesBengaliWordsAsDigits() {
        assertEquals("5 হাজার টাকা", BengaliNumberWords.replaceWithDigits("পাঁচ হাজার টাকা"))
        assertEquals("10 টাকা", BengaliNumberWords.replaceWithDigits("দশ টাকা"))
    }

    @Test
    fun rewritesWholeHundredWordsBeforeTheirPrefix() {
        assertEquals("500 টাকা", BengaliNumberWords.replaceWithDigits("পাঁচশ টাকা"))
        assertEquals("100 টাকা", BengaliNumberWords.replaceWithDigits("একশ টাকা"))
    }

    @Test
    fun rewritesFractionsThatScaleWhateverFollows() {
        assertEquals("1.5 হাজার টাকা", BengaliNumberWords.replaceWithDigits("দেড় হাজার টাকা"))
        assertEquals("2.5 শত টাকা", BengaliNumberWords.replaceWithDigits("আড়াই শত টাকা"))
    }

    @Test
    fun rewritesBanglishWordsWhateverTheirCase() {
        assertEquals("5 hajar taka", BengaliNumberWords.replaceWithDigits("panch hajar taka"))
        assertEquals("2 hajar", BengaliNumberWords.replaceWithDigits("Dui hajar"))
    }

    @Test
    fun aBengaliWordMustStandAloneSoEkaIsNotOne() {
        assertEquals("একা 500", BengaliNumberWords.replaceWithDigits("একা 500"))
    }

    @Test
    fun anAsciiWordMustStandAloneSoEktuIsNotOne() {
        assertEquals("ektu 500", BengaliNumberWords.replaceWithDigits("ektu 500"))
    }

    @Test
    fun leavesDigitsAndUnrelatedWordsAlone() {
        assertEquals("500 taka", BengaliNumberWords.replaceWithDigits("500 taka"))
    }
}
