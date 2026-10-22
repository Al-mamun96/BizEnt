package com.leen.bizentry.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Opens a hand-built version-2 database - the schema that exists on a device which installed the
 * app before this change - and asserts that [BusinessDatabase.MIGRATION_2_3] upgrades it without
 * losing rows and without tripping Room's post-migration schema validation.
 *
 * The version-2 DDL below deliberately carries no DEFAULT clauses, because that is what Room
 * generates for a fresh version-2 install. The other version-2 shape in the field (upgraded from
 * version 1, so carrying DEFAULTs on type/category/transactionDate/paymentMethod) is covered by
 * [MigrationTest] through the 1 -> 2 -> 3 chain.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration2To3Test {

    private lateinit var context: Context
    private var database: BusinessDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getDatabasePath(TEST_DB).let { file ->
            file.parentFile?.mkdirs()
            file.delete()
        }
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migrate2To3_keepsExistingRowsAndLeavesNewColumnsNull() {
        createVersion2Database()

        database = Room.databaseBuilder(context, BusinessDatabase::class.java, TEST_DB)
            .addMigrations(BusinessDatabase.MIGRATION_1_2, BusinessDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        val db = database!!.openHelper.writableDatabase

        db.query("SELECT COUNT(*) FROM business_entries").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2, cursor.getInt(0))
        }

        db.query(
            "SELECT rawText, amount, type, category, paymentMethod, amountMinor, currency, " +
                "transactionDateEpochDay, transactionDateZone, notes, party, reference, " +
                "paymentStatus, dueAmountMinor, settlesTransactionId, updatedAt " +
                "FROM business_entries ORDER BY createdAt ASC"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("legacy v2 entry", cursor.getString(0))
            assertTrue("legacy amount stays null", cursor.isNull(1))
            assertEquals("UNKNOWN", cursor.getString(2))
            assertEquals("other", cursor.getString(3))
            assertEquals("UNKNOWN", cursor.getString(4))

            for (newColumn in 5..15) {
                assertTrue("added column $newColumn must start null", cursor.isNull(newColumn))
            }

            assertTrue(cursor.moveToNext())
            assertEquals("second legacy v2 entry", cursor.getString(0))
        }
    }

    private fun createVersion2Database() {
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(TEST_DB), null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `business_entries` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `rawText` TEXT NOT NULL, " +
                    "`amount` REAL, `type` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                    "`transactionDate` TEXT NOT NULL, `paymentMethod` TEXT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table " +
                    "(id INTEGER PRIMARY KEY, identity_hash TEXT)"
            )
            db.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
                arrayOf<Any>(V2_IDENTITY_HASH)
            )
            db.execSQL(
                "INSERT INTO business_entries " +
                    "(rawText, type, category, transactionDate, paymentMethod, createdAt) " +
                    "VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any>("legacy v2 entry", "UNKNOWN", "other", "", "UNKNOWN", 1_700_000_000_000L)
            )
            db.execSQL(
                "INSERT INTO business_entries " +
                    "(rawText, type, category, transactionDate, paymentMethod, createdAt) " +
                    "VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any>("second legacy v2 entry", "SALE", "cloth", "", "CASH", 1_700_000_000_001L)
            )
            db.version = 2
        }
    }

    private companion object {
        const val TEST_DB = "migration-2-3-test"
        const val V2_IDENTITY_HASH = "ad7388b0fd7f95c1e8be9cb4c06d5b2b"
    }
}
