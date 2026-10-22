
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

    @Query(
        "SELECT SUM(amountMinor) FROM business_entries " +
            "WHERE transactionDateEpochDay BETWEEN :fromEpochDay AND :toEpochDay"
    )
    suspend fun sumAmountMinorInRange(fromEpochDay: Long, toEpochDay: Long): Long?

    @Query(
        "SELECT SUM(amountMinor) FROM business_entries " +
            "WHERE type = :type AND transactionDateEpochDay BETWEEN :fromEpochDay AND :toEpochDay"
    )
    suspend fun sumAmountMinorByType(
        type: TransactionType,
        fromEpochDay: Long,
        toEpochDay: Long
    ): Long?

    @Query(
        "SELECT SUM(amountMinor) FROM business_entries " +
            "WHERE settlement = :settlement " +
            "AND transactionDateEpochDay BETWEEN :fromEpochDay AND :toEpochDay"
    )
    suspend fun sumAmountMinorBySettlement(
        settlement: Settlement,
        fromEpochDay: Long,
        toEpochDay: Long
    ): Long?

    @Query("SELECT SUM(amountMinor) FROM business_entries WHERE settlement = :settlement")
    suspend fun sumAmountMinorBySettlement(settlement: Settlement): Long?

    @Query(
        "SELECT category, COALESCE(SUM(amountMinor), 0) AS totalMinor FROM business_entries " +
            "WHERE transactionDateEpochDay BETWEEN :fromEpochDay AND :toEpochDay " +
            "GROUP BY category ORDER BY totalMinor DESC"
    )
    suspend fun totalsByCategory(fromEpochDay: Long, toEpochDay: Long): List<CategoryTotal>
}
