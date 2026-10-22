package com.leen.bizentry.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun everyTypeRoundTripsThroughItsStoredName() {
        TransactionType.entries.forEach { type ->
            val stored = converters.fromTransactionType(type)
            assertEquals(type.name, stored)
            assertEquals(type, converters.toTransactionType(stored))
        }
    }

    @Test
    fun unknownStoredNameFallsBackToUnknownRatherThanThrowing() {
        assertEquals(
            TransactionType.UNKNOWN,
            converters.toTransactionType("ADDED_IN_A_LATER_VERSION")
        )
    }
}
