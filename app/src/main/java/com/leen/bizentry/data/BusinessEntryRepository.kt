package com.leen.bizentry.data

import kotlinx.coroutines.flow.Flow

interface BusinessEntryRepository {

    val allEntries: Flow<List<BusinessEntry>>

    suspend fun add(entry: BusinessEntry): Long

    suspend fun update(entry: BusinessEntry)

    suspend fun delete(entry: BusinessEntry)

    suspend fun findPossibleDuplicates(rawText: String, sinceMillis: Long): List<BusinessEntry>

    suspend fun sumInRange(fromEpochDay: Long, toEpochDay: Long): Long?

    suspend fun sumByType(type: TransactionType, fromEpochDay: Long, toEpochDay: Long): Long?

    suspend fun sumBySettlement(
        settlement: Settlement,
        fromEpochDay: Long,
        toEpochDay: Long
    ): Long?

    suspend fun totalsByCategory(fromEpochDay: Long, toEpochDay: Long): List<CategoryTotal>
}

class RoomBusinessEntryRepository(private val dao: BusinessEntryDao) : BusinessEntryRepository {

    override val allEntries: Flow<List<BusinessEntry>> = dao.getAllEntries()

    override suspend fun add(entry: BusinessEntry): Long = dao.insert(entry)

    override suspend fun update(entry: BusinessEntry) = dao.update(entry)

    override suspend fun delete(entry: BusinessEntry) = dao.delete(entry)

    override suspend fun findPossibleDuplicates(
        rawText: String,
        sinceMillis: Long
    ): List<BusinessEntry> = dao.findPossibleDuplicates(rawText, sinceMillis)

    override suspend fun sumInRange(fromEpochDay: Long, toEpochDay: Long): Long? =
        dao.sumAmountMinorInRange(fromEpochDay, toEpochDay)

    override suspend fun sumByType(
        type: TransactionType,
        fromEpochDay: Long,
        toEpochDay: Long
    ): Long? = dao.sumAmountMinorByType(type, fromEpochDay, toEpochDay)

    override suspend fun sumBySettlement(
        settlement: Settlement,
        fromEpochDay: Long,
        toEpochDay: Long
    ): Long? = dao.sumAmountMinorBySettlement(settlement, fromEpochDay, toEpochDay)

    override suspend fun totalsByCategory(
        fromEpochDay: Long,
        toEpochDay: Long
    ): List<CategoryTotal> = dao.totalsByCategory(fromEpochDay, toEpochDay)
}
