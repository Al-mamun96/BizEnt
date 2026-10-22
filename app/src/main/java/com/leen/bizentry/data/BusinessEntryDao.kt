
package com.leen.bizentry.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessEntryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: BusinessEntry): Long

    @Query("SELECT * FROM business_entries ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<BusinessEntry>>

    @Delete
    suspend fun delete(entry: BusinessEntry)
}
