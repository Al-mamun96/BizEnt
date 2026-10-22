package com.leen.bizentry.parse

import com.leen.bizentry.data.Settlement
import com.leen.bizentry.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Cash versus credit, which [com.leen.bizentry.data.TransactionType] alone cannot express: a sale
 * may or may not have been paid for, and without that a report can answer neither "how much am I
 * owed" nor "how much cash came in".
 */
class SettlementParsingTest {

    private val today = LocalDate.of(2026, 10, 22)

    @Test
    fun creditIsReadFromAWordThatMeansUnpaid() {
        val parsed = EntryParser.parse("বাকিতে ৫০০০ টাকার কাপড় বিক্রি", today)

        assertEquals(TransactionType.SALE, parsed.type)
        assertEquals(Settlement.CREDIT, parsed.settlement)
    }

    @Test
    fun cashIsReadFromAWordThatMeansPaidNow() {
        val parsed = EntryParser.parse("নগদে ৫০০০ টাকার কাপড় বিক্রি", today)

        assertEquals(TransactionType.SALE, parsed.type)
        assertEquals(Settlement.CASH, parsed.settlement)
    }

    @Test
    fun banglishCreditWordsWorkToo() {
        assertEquals(
            Settlement.CREDIT,
            EntryParser.parse("baki te 5000 takar kapod bikri", today).settlement
        )
    }

    @Test
    fun moneyArrivingIsCashEvenWhenTheWordCreditAppears() {
        val parsed = EntryParser.parse("বাকি ৫০০০ টাকা পেয়েছি", today)

        assertEquals(TransactionType.PAYMENT_IN, parsed.type)
        assertEquals(Settlement.CASH, parsed.settlement)
    }

    @Test
    fun settlementStaysUnstatedWhenTheSentenceIsSilent() {
        assertNull(EntryParser.parse("৫০০ টাকা বিক্রি", today).settlement)
    }
}
