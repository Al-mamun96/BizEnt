
package com.leen.bizentry.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_entries")
data class BusinessEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val rawText: String,

    val amount: Double? = null,

    val type: String = "UNKNOWN",

    val category: String = "অন্যান্য",

    val transactionDate: String = "",

    val paymentMethod: String = "UNKNOWN",

    val createdAt: Long = System.currentTimeMillis()
)
