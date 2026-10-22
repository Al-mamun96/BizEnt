
package com.leen.bizentry.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessEntryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: BusinessEntry): Long

    @Update
    suspend fun update(entry: BusinessEntry)

    @Delete
    suspend fun delete(entry: BusinessEntry)

    @Query("SELECT * FROM business_entries ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<BusinessEntry>>

    @Query("SELECT * FROM business_entries WHERE id = :id")
    suspend fun getById(id: Long): BusinessEntry?

    @Query(
        "SELECT * FROM business_entries " +
            "WHERE transactionDateEpochDay BETWEEN :fromEpochDay AND :toEpochDay " +
            "ORDER BY transactionDateEpochDay DESC, createdAt DESC"
    )
    fun getInDateRange(fromEpochDay: Long, toEpochDay: Long): Flow<List<BusinessEntry>>

    @Query("SELECT * FROM business_entries WHERE type = :type ORDER BY createdAt DESC")
    fun getByType(type: TransactionType): Flow<List<BusinessEntry>>

    @Query("SELECT * FROM business_entries WHERE category = :category ORDER BY createdAt DESC")
    fun getByCategory(category: String): Flow<List<BusinessEntry>>

    @Query(
        "SELECT * FROM business_entries WHERE rawText LIKE '%' || :query || '%' " +
            "ORDER BY createdAt DESC"
    )
    fun search(query: String): Flow<List<BusinessEntry>>

    @Query(
        "SELECT * FROM business_entries " +
            "WHERE rawText = :rawText AND createdAt >= :sinceMillis " +
            "ORDER BY createdAt DESC"
    )
    suspend fun findPossibleDuplicates(rawText: String, sinceMillis: Long): List<BusinessEntry>
}
