package com.leen.bizentry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leen.bizentry.data.BusinessDatabase
import com.leen.bizentry.data.RoomBusinessEntryRepository
import com.leen.bizentry.ui.BusinessEntryViewModel
import com.leen.bizentry.ui.EntryMessage
import com.leen.bizentry.ui.theme.BizEntryTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DATE_PATTERN = "dd-MM-yyyy HH:mm"

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

    val messageText = when (val message = state.message) {
        null -> null
        EntryMessage.EmptyInput -> stringResource(R.string.error_empty_entry)
        EntryMessage.Saved -> stringResource(R.string.save_success)
        is EntryMessage.SaveFailed -> stringResource(
            R.string.save_failed,
            message.detail ?: stringResource(R.string.error_unknown)
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
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving
                ) {
                    Text(
                        if (state.isSaving) stringResource(R.string.saving_in_progress)
                        else stringResource(R.string.action_save)
                    )
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
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("${index + 1}. ${entry.rawText}")

                            Text(
                                text = SimpleDateFormat(
                                    DATE_PATTERN,
                                    Locale.getDefault()
                                ).format(Date(entry.createdAt)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
