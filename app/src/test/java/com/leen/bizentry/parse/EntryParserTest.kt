package com.leen.bizentry.parse

import com.leen.bizentry.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class EntryParserTest {

    private val today = LocalDate.of(2026, 10, 22)

    @Test
    fun readsTheExampleSentenceFromThePrototype() {
        val parsed = EntryParser.parse("আজ ৫০০০ টাকার কাপড় কিনেছি", today)

        assertEquals(500_000L, parsed.amountMinor)
        assertEquals(TransactionType.PURCHASE, parsed.type)
        assertEquals(today, parsed.transactionDate)
        assertEquals("কাপড়", parsed.category)
    }

    @Test
    fun readsBengaliDigits() {
        assertEquals(150_000L, EntryParser.parse("১৫০০ টাকা", today).amountMinor)
    }

    @Test
    fun readsGroupedDigits() {
        assertEquals(500_000L, EntryParser.parse("5,000 taka", today).amountMinor)
    }

    @Test
    fun readsTheKShorthand() {
        assertEquals(500_000L, EntryParser.parse("5k", today).amountMinor)
    }

    @Test
    fun readsATwoDecimalAmount() {
        assertEquals(50_050L, EntryParser.parse("500.50 taka", today).amountMinor)
    }

    @Test
    fun readsScaleWordsInBothLanguages() {
        assertEquals(500_000L, EntryParser.parse("৫ হাজার টাকা", today).amountMinor)
        assertEquals(1_500_000L, EntryParser.parse("15 hazar taka", today).amountMinor)
        assertEquals(20_000_000L, EntryParser.parse("২ লক্ষ টাকা", today).amountMinor)
        assertEquals(50_000L, EntryParser.parse("৫ শত টাকা", today).amountMinor)
    }

    @Test
    fun refusesAnAmountWithTooManyDecimalsInsteadOfRoundingIt() {
        assertNull(EntryParser.parse("১২.৩৪৫ টাকা", today).amountMinor)
    }

    @Test
    fun asksWhenTwoNumbersCouldBothBeTheAmount() {
        assertNull(EntryParser.parse("২ কেজি চাল ৫০০", today).amountMinor)
    }

    @Test
    fun prefersTheNumberBesideTheCurrencyWord() {
        assertEquals(50_000L, EntryParser.parse("২ কেজি চাল ৫০০ টাকা", today).amountMinor)
    }

    @Test
    fun doesNotMistakeADateForAnAmount() {
        assertEquals(50_000L, EntryParser.parse("১৫/১০/২০২৬ তারিখে ৫০০ টাকা", today).amountMinor)
    }

    @Test
    fun readsAnAmountWithNoCurrencyWordWhenItIsTheOnlyNumber() {
        assertEquals(50_000L, EntryParser.parse("চাল ৫০০", today).amountMinor)
    }

    @Test
    fun saleBeatsPurchaseForBikroyWhichContainsKroy() {
        assertEquals(TransactionType.SALE, EntryParser.parse("বিক্রয় ৫০০ টাকা", today).type)
    }

    @Test
    fun recognisesTheCommonTypes() {
        assertEquals(
            TransactionType.PURCHASE,
            EntryParser.parse("৫০০ টাকা কাপড় কিনেছি", today).type
        )
        assertEquals(TransactionType.SALE, EntryParser.parse("৫০০ টাকা বিক্রি", today).type)
        assertEquals(TransactionType.EXPENSE, EntryParser.parse("৫০০ টাকা খরচ", today).type)
        assertEquals(TransactionType.INCOME, EntryParser.parse("৫০০ টাকা আয়", today).type)
        assertEquals(TransactionType.PAYMENT_IN, EntryParser.parse("৫০০ টাকা পেয়েছি", today).type)
        assertEquals(TransactionType.PAYMENT_OUT, EntryParser.parse("৫০০ টাকা দিয়েছি", today).type)
    }

    @Test
    fun leavesTheTypeUnknownRatherThanGuessing() {
        assertEquals(TransactionType.UNKNOWN, EntryParser.parse("৫০০ টাকা", today).type)
    }

    @Test
    fun readsRelativeDatesInBothLanguages() {
        assertEquals(today, EntryParser.parse("আজ ৫০০ টাকা", today).transactionDate)
        assertEquals(today.minusDays(1), EntryParser.parse("গতকাল ৫০০ টাকা", today).transactionDate)
        assertEquals(today.plusDays(1), EntryParser.parse("আগামীকাল ৫০০ টাকা", today).transactionDate)
        assertEquals(today.minusDays(1), EntryParser.parse("gotokal 500 taka", today).transactionDate)
    }

    @Test
    fun leavesTheDateNullWhenItWasNotStated() {
        assertNull(EntryParser.parse("৫০০ টাকা", today).transactionDate)
    }

    @Test
    fun banglishKeywordsNeedWholeWordsSoBajarIsNotReadAsToday() {
        assertNull(EntryParser.parse("bajar theke 500 taka", today).transactionDate)
    }

    @Test
    fun recognisesIncomeWrittenWithTheDecomposedNuktaForm() {
        val decomposedAy = "\u0986\u09AF\u09BC"

        assertEquals(
            TransactionType.INCOME,
            EntryParser.parse("$decomposedAy ৫০০ টাকা", today).type
        )
    }

    @Test
    fun keepsTheOriginalTextSoEntriesCanBeReanalysedLater() {
        val text = "আজ ৫০০০ টাকার কাপড় কিনেছি"

        assertEquals(text, EntryParser.parse(text, today).rawText)
    }
}
