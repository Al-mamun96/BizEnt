package com.leen.bizentry.data

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(storedName: String): TransactionType =
        TransactionType.entries.firstOrNull { it.name == storedName } ?: TransactionType.UNKNOWN
}
