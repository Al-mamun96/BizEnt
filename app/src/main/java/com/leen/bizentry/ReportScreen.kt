package com.leen.bizentry

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leen.bizentry.data.Money
import com.leen.bizentry.data.ReportPeriod
import com.leen.bizentry.data.TransactionType
import com.leen.bizentry.ui.ReportViewModel

private val ReportPeriod.labelRes: Int
    get() = when (this) {
        ReportPeriod.TODAY -> R.string.period_today
        ReportPeriod.THIS_WEEK -> R.string.period_this_week
        ReportPeriod.THIS_MONTH -> R.string.period_this_month
        ReportPeriod.LAST_MONTH -> R.string.period_last_month
    }

@Composable
fun ReportScreen(viewModel: ReportViewModel) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.report_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportPeriod.entries.forEach { period ->
                        val label = stringResource(period.labelRes)
                        if (period == summary.period) {
                            Button(onClick = { viewModel.select(period) }) { Text(label) }
                        } else {
                            OutlinedButton(onClick = { viewModel.select(period) }) { Text(label) }
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.report_total, Money.format(summary.totalMinor)))
            }

            item {
                Text(stringResource(R.string.report_cash, Money.format(summary.cashMinor)))
            }

            item {
                Text(stringResource(R.string.report_credit, Money.format(summary.creditMinor)))
            }

            if (summary.notStatedMinor != 0L) {
                item {
                    Text(
                        stringResource(
                            R.string.report_not_stated,
                            Money.format(summary.notStatedMinor)
                        )
                    )
                }
            }

            summary.byType
                .filter { (_, total) -> total != 0L }
                .forEach { (type, total) ->
                    item {
                        Text(
                            text = stringResource(type.labelRes) + "  " + Money.format(total),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

            item {
                Text(
                    text = stringResource(R.string.report_by_category),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (summary.byCategory.isEmpty()) {
                item { Text(stringResource(R.string.report_empty)) }
            } else {
                summary.byCategory.forEach { categoryTotal ->
                    item {
                        Text(
                            "${categoryTotal.category}  ${Money.format(categoryTotal.totalMinor)}"
                        )
                    }
                }
            }
        }
    }
}
