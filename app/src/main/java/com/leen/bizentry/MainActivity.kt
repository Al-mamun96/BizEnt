package com.leen.bizentry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leen.bizentry.data.BusinessDatabase
import com.leen.bizentry.data.BusinessEntry
import com.leen.bizentry.data.Money
import com.leen.bizentry.data.RoomBusinessEntryRepository
import com.leen.bizentry.data.TransactionDates
import com.leen.bizentry.data.TransactionType
import com.leen.bizentry.ui.BusinessEntryViewModel
import com.leen.bizentry.ui.EntryMessage
import com.leen.bizentry.ui.theme.BizEntryTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessEntryScreen(viewModel: BusinessEntryViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var choosingDate by remember { mutableStateOf(false) }
    var choosingType by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BusinessEntry?>(null) }

    val formatter = remember {
        DateTimeFormatter.ofPattern(FIELD_DATE_PATTERN, Locale.getDefault())
    }
    val selectedDate = LocalDate.ofEpochDay(state.transactionDateEpochDay)

    val messageText = when (val message = state.message) {
        null -> null
        EntryMessage.EmptyInput -> stringResource(R.string.error_empty_entry)
        EntryMessage.AmountInvalid -> stringResource(R.string.error_amount_invalid)
        EntryMessage.Saved -> stringResource(R.string.save_success)
        EntryMessage.Updated -> stringResource(R.string.update_success)
        EntryMessage.Deleted -> stringResource(R.string.delete_success)
        is EntryMessage.SaveFailed -> stringResource(
            R.string.save_failed,
            message.detail ?: stringResource(R.string.error_unknown)
        )
    }

    if (choosingDate) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = TransactionDates.toPickerMillis(selectedDate)
        )

        DatePickerDialog(
            onDismissRequest = { choosingDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { picked ->
                            viewModel.onDateChange(TransactionDates.fromPickerMillis(picked))
                        }
                        choosingDate = false
                    }
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { choosingDate = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.screen_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            item {
                Text(stringResource(R.string.entry_hint))
            }

            item {
                OutlinedTextField(
                    value = state.text,
                    onValueChange = viewModel::onTextChange,
                    label = { Text(stringResource(R.string.entry_label)) },
                    placeholder = { Text(stringResource(R.string.entry_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    enabled = !state.isSaving
                )
            }

            item {
                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text(stringResource(R.string.amount_label)) },
                    placeholder = { Text(stringResource(R.string.amount_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving
                )
            }

            item {
                Box {
                    OutlinedButton(
                        onClick = { choosingType = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving
                    ) {
                        Text(
                            stringResource(
                                R.string.type_field,
                                stringResource(state.type.labelRes)
                            )
                        )
                    }

                    DropdownMenu(
                        expanded = choosingType,
                        onDismissRequest = { choosingType = false }
                    ) {
                        selectableTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(stringResource(type.labelRes)) },
                                onClick = {
                                    viewModel.onTypeChange(type)
                                    choosingType = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { choosingDate = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving
                ) {
                    Text(stringResource(R.string.date_field, formatter.format(selectedDate)))
                }
            }

            item {
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving
                ) {
                    Text(
                        when {
                            state.isSaving -> stringResource(R.string.saving_in_progress)
                            state.editingId != null -> stringResource(R.string.action_update)
                            else -> stringResource(R.string.action_save)
                        }
                    )
                }
            }

            if (state.editingId != null) {
                item {
                    OutlinedButton(
                        onClick = viewModel::cancelEditing,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }

            if (messageText != null) {
                item { Text(messageText) }
            }

            item { HorizontalDivider() }

            item {
                Text(
                    text = stringResource(R.string.entry_list_header, state.entries.size),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (state.entries.isEmpty()) {
                item { Text(stringResource(R.string.empty_state)) }
            } else {
                itemsIndexed(state.entries, key = { _, entry -> entry.id }) { index, entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.startEditing(entry) }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("${index + 1}. ${entry.rawText}")

                            entry.amountMinor?.let { minorUnits ->
                                Text(
                                    text = Money.format(minorUnits),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Text(
                                text = java.text.SimpleDateFormat(
                                    ENTRY_DATE_PATTERN,
                                    Locale.getDefault()
                                ).format(java.util.Date(entry.createdAt)),
                                style = MaterialTheme.typography.bodySmall
                            )

                            TextButton(
                                onClick = { pendingDelete = entry },
                                enabled = !state.isSaving
                            ) {
                                Text(stringResource(R.string.action_delete))
                            }
                        }
                    }
                }
            }
        }
    }
}
