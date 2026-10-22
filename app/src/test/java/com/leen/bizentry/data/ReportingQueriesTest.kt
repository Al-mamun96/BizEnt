package com.leen.bizentry.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The aggregation half of Phase 4. These are deliberately mechanical sums over stored columns:
 * what counts as "cash in" or "revenue" is an accounting decision for the report layer, not
 * something to bake into SQL here.
 *
 * The case worth watching is rows recorded before Settlement existed. They carry null, so a
 * settlement-filtered sum does not quietly count them as cash.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReportingQueriesTest {

    private lateinit var database: BusinessDatabase
    private lateinit var dao: BusinessEntryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BusinessDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.businessEntryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun entry(
        amountMinor: Long?,
        epochDay: Long?,
        type: TransactionType = TransactionType.SALE,
        settlement: Settlement? = null,
        category: String = DEFAULT_CATEGORY
    ) = BusinessEntry(
        rawText = "entry",
        amountMinor = amountMinor,
        type = type,
        settlement = settlement,
        category = category,
        transactionDateEpochDay = epochDay
    )

    @Test
    fun sumsOnlyWithinTheInclusiveRange() = runBlocking {
        dao.insert(entry(100_000L, 99L))
        dao.insert(entry(200_000L, 100L))
        dao.insert(entry(300_000L, 200L))
        dao.insert(entry(400_000L, 201L))

        assertEquals(500_000L, dao.sumAmountMinorInRange(100L, 200L))
    }

    @Test
    fun rowsWithNoDateAreLeftOutOfAPeriodSum() = runBlocking {
        dao.insert(entry(100_000L, 150L))
        dao.insert(entry(999_000L, epochDay = null))

        assertEquals(100_000L, dao.sumAmountMinorInRange(0L, 999L))
    }

    @Test
    fun aPeriodWithNothingInItSumsToNullRatherThanZero() = runBlocking {
        assertNull(dao.sumAmountMinorInRange(0L, 999L))
    }

    @Test
    fun sumsByTypeWithinARange() = runBlocking {
        dao.insert(entry(100_000L, 150L, type = TransactionType.SALE))
        dao.insert(entry(250_000L, 150L, type = TransactionType.SALE))
        dao.insert(entry(999_000L, 150L, type = TransactionType.EXPENSE))

        assertEquals(350_000L, dao.sumAmountMinorByType(TransactionType.SALE, 0L, 999L))
        assertEquals(999_000L, dao.sumAmountMinorByType(TransactionType.EXPENSE, 0L, 999L))
        assertNull(dao.sumAmountMinorByType(TransactionType.TRANSFER, 0L, 999L))
    }

    @Test
    fun creditAndCashAreSummedSeparately() = runBlocking {
        dao.insert(entry(500_000L, 150L, settlement = Settlement.CREDIT))
        dao.insert(entry(200_000L, 150L, settlement = Settlement.CASH))

        assertEquals(500_000L, dao.sumAmountMinorBySettlement(Settlement.CREDIT))
        assertEquals(200_000L, dao.sumAmountMinorBySettlement(Settlement.CASH))
    }

    @Test
    fun rowsRecordedBeforeSettlementExistedCountAsNeitherCashNorCredit() = runBlocking {
        dao.insert(entry(500_000L, 150L, settlement = null))

        assertNull(dao.sumAmountMinorBySettlement(Settlement.CREDIT))
        assertNull(dao.sumAmountMinorBySettlement(Settlement.CASH))
    }

    @Test
    fun categoriesAreTotalledAndOrderedBiggestFirst() = runBlocking {
        dao.insert(entry(100_000L, 150L, category = "খাবার"))
        dao.insert(entry(500_000L, 150L, category = "কাপড়"))
        dao.insert(entry(200_000L, 150L, category = "খাবার"))

        assertEquals(
            listOf(CategoryTotal("কাপড়", 500_000L), CategoryTotal("খাবার", 300_000L)),
            dao.totalsByCategory(0L, 999L)
        )
    }

    @Test
    fun aCategoryWithNoAmountsReportsZeroRatherThanBeingDropped() = runBlocking {
        dao.insert(entry(amountMinor = null, epochDay = 150L, category = "ওষুধ"))

        assertEquals(listOf(CategoryTotal("ওষুধ", 0L)), dao.totalsByCategory(0L, 999L))
    }
}
