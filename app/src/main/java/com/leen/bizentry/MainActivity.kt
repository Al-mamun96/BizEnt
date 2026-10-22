package com.leen.bizentry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leen.bizentry.data.BusinessDatabase
import com.leen.bizentry.data.BusinessEntry
import com.leen.bizentry.data.Money
import com.leen.bizentry.data.RoomBusinessEntryRepository
import com.leen.bizentry.data.TransactionType
import com.leen.bizentry.parse.EntryParser
import com.leen.bizentry.parse.ParsedEntry
import com.leen.bizentry.ui.BusinessEntryViewModel
import com.leen.bizentry.ui.EntryMessage
import com.leen.bizentry.ui.theme.BizEntryTheme
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private const val ENTRY_DATE_PATTERN = "dd-MM-yyyy HH:mm"
private const val FIELD_DATE_PATTERN = "dd MMM yyyy"

private val selectableTypes = TransactionType.entries.filter { it != TransactionType.UNKNOWN }

private val TransactionType.labelRes: Int
    get() = when (this) {
        TransactionType.UNKNOWN -> R.string.type_unset
        TransactionType.SALE -> R.string.type_sale
        TransactionType.PURCHASE -> R.string.type_purchase
        TransactionType.EXPENSE -> R.string.type_expense
        TransactionType.INCOME -> R.string.type_income
        TransactionType.PAYMENT_IN -> R.string.type_payment_in
        TransactionType.PAYMENT_OUT -> R.string.type_payment_out
        TransactionType.TRANSFER -> R.string.type_transfer
    }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = RoomBusinessEntryRepository(
            BusinessDatabase.getDatabase(applicationContext).businessEntryDao()
        )

        setContent {
            BizEntryTheme {
                val viewModel: BusinessEntryViewModel = viewModel(
                    factory = BusinessEntryViewModel.factory(repository)
                )
                BusinessEntryScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BusinessEntryScreen(viewModel: BusinessEntryViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var pendingDelete by remember { mutableStateOf<BusinessEntry?>(null) }

    val formatter = remember {
        DateTimeFormatter.ofPattern(FIELD_DATE_PATTERN, Locale.getDefault())
    }

    val messageText = when (val message = state.message) {
        null -> null
        EntryMessage.EmptyInput -> stringResource(R.string.error_empty_entry)
        EntryMessage.AmountNotFound -> stringResource(R.string.ask_amount)
        EntryMessage.AmountInvalid -> stringResource(R.string.error_amount_invalid)
        EntryMessage.Saved -> stringResource(R.string.save_success)
        EntryMessage.Updated -> stringResource(R.string.update_success)
        EntryMessage.Deleted -> stringResource(R.string.delete_success)
        is EntryMessage.SaveFailed -> stringResource(
            R.string.save_failed,
            message.detail ?: stringResource(R.string.error_unknown)
        )
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(entry)
                        pendingDelete = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.screen_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                item { Text(stringResource(R.string.entry_hint)) }

                if (state.entries.isEmpty() && state.draft == null) {
                    item { Text(stringResource(R.string.empty_state)) }
                }

                itemsIndexed(state.entries, key = { _, entry -> entry.id }) { _, entry ->
                    EntryBubble(
                        entry = entry,
                        formatter = formatter,
                        enabled = !state.isSaving,
                        onEdit = { viewModel.startEditing(entry) },
                        onDelete = { pendingDelete = entry }
                    )
                }

                state.draft?.let { draft ->
                    item {
                        DraftBubble(
                            draft = draft,
                            editing = state.editingId != null,
                            formatter = formatter,
                            enabled = !state.isSaving,
                            onTypeChange = viewModel::onDraftTypeChange,
                            onCategoryChange = viewModel::onDraftCategoryChange,
                            onConfirm = viewModel::confirmDraft,
                            onCancel = viewModel::cancelDraft
                        )
                    }
                }
            }

            if (messageText != null) {
                Text(
                    text = messageText,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::onInputChange,
                    placeholder = { Text(stringResource(R.string.entry_placeholder)) },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    enabled = !state.isSaving
                )

                Button(
                    onClick = viewModel::send,
                    enabled = !state.isSaving
                ) {
                    Text(
                        if (state.isSaving) stringResource(R.string.saving_in_progress)
                        else stringResource(R.string.action_send)
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryBubble(
    entry: BusinessEntry,
    formatter: DateTimeFormatter,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(entry.rawText)

            entry.amountMinor?.let { minorUnits ->
                Text(
                    text = stringResource(R.string.draft_amount, Money.format(minorUnits)),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = entry.transactionDateEpochDay
                    ?.let { formatter.format(LocalDate.ofEpochDay(it)) }
                    ?: SimpleDateFormat(ENTRY_DATE_PATTERN, Locale.getDefault())
                        .format(Date(entry.createdAt)),
                style = MaterialTheme.typography.bodySmall
            )

            TextButton(onClick = onDelete, enabled = enabled) {
                Text(stringResource(R.string.action_delete))
            }
        }
    }
}

@Composable
private fun DraftBubble(
    draft: ParsedEntry,
    editing: Boolean,
    formatter: DateTimeFormatter,
    enabled: Boolean,
    onTypeChange: (TransactionType) -> Unit,
    onCategoryChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(draft.rawText)

            if (draft.amountMinor != null) {
                Text(
                    text = stringResource(R.string.draft_amount, Money.format(draft.amountMinor)),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = stringResource(R.string.ask_amount),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            CorrectionRow(
                text = stringResource(R.string.type_field, stringResource(draft.type.labelRes)),
                options = selectableTypes.map { type ->
                    stringResource(type.labelRes) to { onTypeChange(type) }
                },
                enabled = enabled
            )

            draft.transactionDate?.let { date ->
                Text(stringResource(R.string.date_field, formatter.format(date)))
            }

            CorrectionRow(
                text = stringResource(
                    R.string.draft_category,
                    draft.category ?: stringResource(R.string.type_unset)
                ),
                options = EntryParser.knownCategories.map { category ->
                    category to { onCategoryChange(category) }
                },
                enabled = enabled
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onConfirm,
                    enabled = enabled && draft.amountMinor != null
                ) {
                    Text(
                        if (editing) stringResource(R.string.action_update)
                        else stringResource(R.string.action_save)
                    )
                }

                OutlinedButton(onClick = onCancel, enabled = enabled) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }
}

@Composable
private fun CorrectionRow(
    text: String,
    options: List<Pair<String, () -> Unit>>,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }, enabled = enabled) {
            Text(text)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (label, onPick) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onPick()
                        expanded = false
                    }
                )
            }
        }
    }
}
