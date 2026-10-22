package com.leen.bizentry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leen.bizentry.data.BusinessEntryRepository
import com.leen.bizentry.data.CategoryTotal
import com.leen.bizentry.data.EpochDayRange
import com.leen.bizentry.data.ReportPeriod
import com.leen.bizentry.data.Settlement
import com.leen.bizentry.data.TransactionType
import com.leen.bizentry.data.rangeIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * A mechanical summary: it reports what the columns say and does not decide what counts as revenue
 * or as cash in. Those are accounting definitions, and they belong somewhere readable rather than
 * encoded in SQL.
 *
 * [notStatedMinor] exists so entries recorded before Settlement existed are visible instead of
 * silently missing. Those rows carry null, so they appear in the total but in neither cash nor
 * credit, and the difference is exactly them.
 */
data class ReportSummary(
    val period: ReportPeriod = ReportPeriod.THIS_MONTH,
    val range: EpochDayRange = EpochDayRange(0L, 0L),
    val totalMinor: Long = 0L,
    val byType: Map<TransactionType, Long> = emptyMap(),
    val cashMinor: Long = 0L,
    val creditMinor: Long = 0L,
    val notStatedMinor: Long = 0L,
    val byCategory: List<CategoryTotal> = emptyList()
)

class ReportViewModel(
    private val repository: BusinessEntryRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val firstDayOfWeek: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
) : ViewModel() {

    private val mutableSummary = MutableStateFlow(ReportSummary())
    val summary: StateFlow<ReportSummary> = mutableSummary.asStateFlow()

    init {
        select(ReportPeriod.THIS_MONTH)
    }

    fun select(period: ReportPeriod) {
        viewModelScope.launch {
            val range = period.rangeIn(LocalDate.now(clock), firstDayOfWeek)
            val total = repository.sumInRange(range.from, range.to) ?: 0L
            val cash = repository.sumBySettlement(Settlement.CASH, range.from, range.to) ?: 0L
            val credit = repository.sumBySettlement(Settlement.CREDIT, range.from, range.to) ?: 0L

            mutableSummary.value = ReportSummary(
                period = period,
                range = range,
                totalMinor = total,
                byType = TransactionType.entries
                    .filter { it != TransactionType.UNKNOWN }
                    .associateWith { repository.sumByType(it, range.from, range.to) ?: 0L },
                cashMinor = cash,
                creditMinor = credit,
                notStatedMinor = total - cash - credit,
                byCategory = repository.totalsByCategory(range.from, range.to)
            )
        }
    }

    companion object {
        fun factory(repository: BusinessEntryRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ReportViewModel(repository) }
            }
    }
}
