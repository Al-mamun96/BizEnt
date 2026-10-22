package com.leen.bizentry.data

import androidx.room.ColumnInfo

data class CategoryTotal(
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "totalMinor") val totalMinor: Long
)
