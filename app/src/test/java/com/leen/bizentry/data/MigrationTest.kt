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
 * Opens a hand-built version-1 database with the real [Room.databaseBuilder] and asserts that
 * [BusinessDatabase.MIGRATION_1_2] and [BusinessDatabase.MIGRATION_2_3] carry it all the way to the
 * current version without losing rows and without tripping Room's post-migration schema validation.
 *
 * This is the same code path a user's device takes when the app is updated, so a successful open
 * proves the migration is structurally valid and the assertions prove the existing rows survive.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

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
    fun migrate1To3_keepsExistingRowsAndAppliesColumnDefaults() {
        createVersion1Database()

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
            "SELECT rawText, amount, type, category, transactionDate, paymentMethod " +
                "FROM business_entries ORDER BY createdAt ASC"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("আজ ৫০০০ টাকার কাপড় কিনেছি", cursor.getString(0))
            assertTrue("legacy rows must keep a null amount", cursor.isNull(1))
            assertEquals("UNKNOWN", cursor.getString(2))
            assertEquals("অন্যান্য", cursor.getString(3))
            assertEquals("", cursor.getString(4))
            assertEquals("UNKNOWN", cursor.getString(5))

            assertTrue(cursor.moveToNext())
            assertEquals("legacy free text entry", cursor.getString(0))
        }
    }

    /**
     * v1 held only `id`, `rawText` and `createdAt`; the 1 -> 2 migration adds the other five
     * columns. `room_master_table` is pre-seeded because Room refuses to open a database it
     * cannot identify. The stored hash is deliberately arbitrary: Room overwrites the identity
     * hash as part of the upgrade and validates the table structure against the entity rather
     * than against this value.
     */
    private fun createVersion1Database() {
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(TEST_DB), null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `business_entries` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `rawText` TEXT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table " +
                    "(id INTEGER PRIMARY KEY, identity_hash TEXT)"
            )
            db.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
                arrayOf<Any>(V1_IDENTITY_HASH)
            )
            db.execSQL(
                "INSERT INTO business_entries (rawText, createdAt) VALUES (?, ?)",
                arrayOf<Any>("আজ ৫০০০ টাকার কাপড় কিনেছি", 1_700_000_000_000L)
            )
            db.execSQL(
                "INSERT INTO business_entries (rawText, createdAt) VALUES (?, ?)",
                arrayOf<Any>("legacy free text entry", 1_700_000_000_001L)
            )
            db.version = 1
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
        const val V1_IDENTITY_HASH = "aa0f862390713279b39b797abc851a76"
    }
}
