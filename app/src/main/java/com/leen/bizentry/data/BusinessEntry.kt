
package com.leen.bizentry.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "business_entries",
    indices = [
        Index("transactionDateEpochDay"),
        Index("type"),
        Index("category"),
        Index("paymentMethod")
    ]
)
data class BusinessEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val rawText: String,

    val amount: Double? = null,

    val type: TransactionType = TransactionType.UNKNOWN,

    val category: String = "অন্যান্য",

    val transactionDate: String = "",

    val paymentMethod: String = "UNKNOWN",

    val createdAt: Long = System.currentTimeMillis(),

    val amountMinor: Long? = null,

    val currency: String? = null,

    val transactionDateEpochDay: Long? = null,

    val transactionDateZone: String? = null,

    val notes: String? = null,

    val party: String? = null,

    val reference: String? = null,

    val paymentStatus: String? = null,

    val dueAmountMinor: Long? = null,

    val settlesTransactionId: Long? = null,

    val updatedAt: Long? = null
)
