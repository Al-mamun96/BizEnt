package com.leen.bizentry

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.leen.bizentry.data.BusinessDatabase
import com.leen.bizentry.data.BusinessEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real Room + SQLite stack on a device, against the production [BusinessEntry] and
 * [com.leen.bizentry.data.BusinessEntryDao].
 *
 * A Robolectric test cannot prove this: the point here is that a file-backed database on real
 * Android storage survives closing and reopening the connection. That is the path a user takes
 * every time the app is started after being killed, and it is the check that would catch a
 * schema that Room builds but SQLite cannot reopen.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseRoundTripTest {

    private lateinit var context: Context
    private var database: BusinessDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun entryRoundTripsAndSurvivesReopen() = runBlocking {
        val entry = BusinessEntry(
            rawText = "আজ ৫০০০ টাকার কাপড় কিনেছি",
            amount = 5000.75,
            type = "EXPENSE",
            category = "কাপড়",
            transactionDate = "2026-10-22",
            paymentMethod = "CASH"
        )

        database = open()
        val id = database!!.businessEntryDao().insert(entry)
        assertTrue("insert must assign an id", id > 0)

        val afterInsert = database!!.businessEntryDao().getAllEntries().first()
        assertEquals(1, afterInsert.size)
        assertEquals(entry.copy(id = id), afterInsert.first())

        database!!.close()
        database = open()

        val afterReopen = database!!.businessEntryDao().getAllEntries().first()
        assertEquals("row must survive closing and reopening", 1, afterReopen.size)
        assertEquals(entry.copy(id = id), afterReopen.first())
    }

    private fun open(): BusinessDatabase =
        Room.databaseBuilder(context, BusinessDatabase::class.java, TEST_DB).build()

    private companion object {
        const val TEST_DB = "instrumented-round-trip-test"
    }
}
