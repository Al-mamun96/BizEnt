package com.leen.bizentry.data

import kotlinx.coroutines.flow.Flow

interface BusinessEntryRepository {

    val allEntries: Flow<List<BusinessEntry>>

    suspend fun add(entry: BusinessEntry): Long

    suspend fun update(entry: BusinessEntry)

    suspend fun delete(entry: BusinessEntry)

    suspend fun findPossibleDuplicates(rawText: String, sinceMillis: Long): List<BusinessEntry>
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
}
