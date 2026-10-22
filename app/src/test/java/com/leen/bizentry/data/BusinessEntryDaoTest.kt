package com.leen.bizentry.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises every [BusinessEntryDao] query against a real in-memory Room database. Opening it also
 * validates the fresh-install schema (including the indices), which the migration tests do not
 * cover because they start from a hand-built older database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BusinessEntryDaoTest {

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
        rawText: String,
        createdAt: Long,
        type: TransactionType = TransactionType.UNKNOWN,
        category: String = "other",
        epochDay: Long? = null
    ) = BusinessEntry(
        rawText = rawText,
        type = type,
        category = category,
        transactionDateEpochDay = epochDay,
        createdAt = createdAt
    )

    @Test
    fun getAllEntriesReturnsNewestFirst() = runBlocking {
        dao.insert(entry("oldest", 1))
        dao.insert(entry("middle", 2))
        dao.insert(entry("newest", 3))

        assertEquals(
            listOf("newest", "middle", "oldest"),
            dao.getAllEntries().first().map { it.rawText }
        )
    }

    @Test
    fun updateReplacesTheStoredRowInsteadOfAddingOne() = runBlocking {
        val id = dao.insert(entry("before", 1))

        dao.update(entry("after", 1).copy(id = id))

        assertEquals("after", dao.getById(id)?.rawText)
        assertEquals(1, dao.getAllEntries().first().size)
    }

    @Test
    fun getByIdReturnsNullWhenTheRowIsGone() = runBlocking {
        val id = dao.insert(entry("temporary", 1))
        dao.delete(dao.getById(id)!!)

        assertNull(dao.getById(id))
    }

    @Test
    fun getInDateRangeIsInclusiveAndSkipsRowsWithNoDate() = runBlocking {
        dao.insert(entry("before range", 1, epochDay = 99))
        dao.insert(entry("range start", 2, epochDay = 100))
        dao.insert(entry("range end", 3, epochDay = 200))
        dao.insert(entry("after range", 4, epochDay = 201))
        dao.insert(entry("legacy row with no date", 5, epochDay = null))

        assertEquals(
            listOf("range end", "range start"),
            dao.getInDateRange(100, 200).first().map { it.rawText }
        )
    }

    @Test
    fun getByTypeFiltersOnTheStoredTypeName() = runBlocking {
        dao.insert(entry("a sale", 1, type = TransactionType.SALE))
        dao.insert(entry("an expense", 2, type = TransactionType.EXPENSE))
        dao.insert(entry("another sale", 3, type = TransactionType.SALE))

        assertEquals(
            listOf("another sale", "a sale"),
            dao.getByType(TransactionType.SALE).first().map { it.rawText }
        )
    }

    @Test
    fun getByCategoryFilters() = runBlocking {
        dao.insert(entry("cloth", 1, category = "cloth"))
        dao.insert(entry("rice", 2, category = "food"))

        assertEquals(
            listOf("cloth"),
            dao.getByCategory("cloth").first().map { it.rawText }
        )
    }

    @Test
    fun searchMatchesASubstringAnywhereInTheRawText() = runBlocking {
        dao.insert(entry("bought cloth today", 1))
        dao.insert(entry("paid rent", 2))
        dao.insert(entry("sold cloth", 3))

        assertEquals(
            listOf("sold cloth", "bought cloth today"),
            dao.search("cloth").first().map { it.rawText }
        )
    }

    @Test
    fun findPossibleDuplicatesFlagsTheSameTextInsideTheWindow() = runBlocking {
        dao.insert(entry("paid rent", 1_000))

        val found = dao.findPossibleDuplicates("paid rent", sinceMillis = 900)

        assertEquals(1, found.size)
        assertEquals("paid rent", found.first().rawText)
    }

    @Test
    fun findPossibleDuplicatesIgnoresMatchesOlderThanTheWindow() = runBlocking {
        dao.insert(entry("paid rent", 1_000))

        assertTrue(dao.findPossibleDuplicates("paid rent", sinceMillis = 1_001).isEmpty())
    }

    @Test
    fun findPossibleDuplicatesIgnoresUnrelatedText() = runBlocking {
        dao.insert(entry("paid rent", 1_000))

        assertTrue(dao.findPossibleDuplicates("bought cloth", sinceMillis = 0).isEmpty())
    }
}
