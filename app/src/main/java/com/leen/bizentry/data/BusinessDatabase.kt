
package com.leen.bizentry.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BusinessEntry::class],
    version = 2,
    exportSchema = false
)
abstract class BusinessDatabase : RoomDatabase() {

    abstract fun businessEntryDao(): BusinessEntryDao

    companion object {
        @Volatile
        private var INSTANCE: BusinessDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
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

        fun getDatabase(context: Context): BusinessDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BusinessDatabase::class.java,
                    "biz_entry_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
