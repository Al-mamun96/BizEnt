
package com.leen.bizentry.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BusinessEntry::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BusinessDatabase : RoomDatabase() {

    abstract fun businessEntryDao(): BusinessEntryDao

    companion object {
        @Volatile
        private var INSTANCE: BusinessDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE business_entries ADD COLUMN amount REAL"
                )
                db.execSQL(
                    "ALTER TABLE business_entries ADD COLUMN type TEXT NOT NULL DEFAULT 'UNKNOWN'"
                )
                db.execSQL(
                    "ALTER TABLE business_entries ADD COLUMN category TEXT NOT NULL DEFAULT 'অন্যান্য'"
                )
                db.execSQL(
                    "ALTER TABLE business_entries ADD COLUMN transactionDate TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE business_entries ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'UNKNOWN'"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE business_entries ADD COLUMN amountMinor INTEGER"
                )
                db.execSQL(
                    "ALTER TABLE business_entries ADD COLUMN currency TEXT"
                )
                db.execSQL(
                    "ALTER TABLE business_entries ADD COLUMN transactionDateEpochDay INTEGER"
                )
                db.execSQL(
                    "ALTER TABLE business_entries ADD COLUMN transactionDateZone TEXT"
                )
                db.execSQL(
                    "ALTER TABLE business_entries ADD COLUMN notes TEXT"
                )
                db.execSQL(
                    "ALTER TABLE business_entries ADD COLUMN party TEXT"
                )
                db.execSQL(
                    "ALTER TABLE business_entries ADD COLUMN reference TEXT"
                )
                db.execSQL(
                    "ALTER TABLE business_entries ADD COLUMN paymentStatus TEXT"
                )
                db.execSQL(
                    "ALTER TABLE business_entries ADD COLUMN dueAmountMinor INTEGER"
                )
                db.execSQL(
                    "ALTER TABLE business_entries ADD COLUMN settlesTransactionId INTEGER"
                )
                db.execSQL(
                    "ALTER TABLE business_entries ADD COLUMN updatedAt INTEGER"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_business_entries_transactionDateEpochDay " +
                        "ON business_entries (transactionDateEpochDay)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_business_entries_type " +
                        "ON business_entries (type)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_business_entries_category " +
                        "ON business_entries (category)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_business_entries_paymentMethod " +
                        "ON business_entries (paymentMethod)"
                )
            }
        }

        fun getDatabase(context: Context): BusinessDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BusinessDatabase::class.java,
                    "biz_entry_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
