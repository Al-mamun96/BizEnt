package com.leen.bizentry.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The Material3 date picker reports its selection as milliseconds at midnight UTC, so the
 * conversion has to be done in UTC too. Reading those milliseconds in the device's own zone shifts
 * the day by one for any zone west of UTC.
 */
object TransactionDates {

    fun fromPickerMillis(pickerMillis: Long): LocalDate =
        Instant.ofEpochMilli(pickerMillis).atZone(ZoneOffset.UTC).toLocalDate()

    fun toPickerMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
