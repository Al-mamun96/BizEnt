
package com.leen.bizentry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.leen.bizentry.data.BusinessDatabase
import com.leen.bizentry.data.BusinessEntry
import com.leen.bizentry.data.BusinessEntryDao
import com.leen.bizentry.ui.theme.BizEntryTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dao = BusinessDatabase.getDatabase(applicationContext)
            .businessEntryDao()

        val unknownError = getString(R.string.error_unknown)

        setContent {
            BizEntryTheme {
                BusinessEntryScreen(
                    dao = dao,
                    onSave = { text, onResult ->
                        lifecycleScope.launch {
                            try {
                                dao.insert(BusinessEntry(rawText = text))
                                onResult(true, null)
                            } catch (e: Exception) {
                                onResult(
                                    false,
                                    e.localizedMessage ?: unknownError
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BusinessEntryScreen(
    dao: BusinessEntryDao,
    onSave: (String, (Boolean, String?) -> Unit) -> Unit
) {
    var entry by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val savedEntries by dao.getAllEntries()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_title),
                style = MaterialTheme.typography.headlineSmall
            )

            Text(stringResource(R.string.entry_hint))

            OutlinedTextField(
                value = entry,
                onValueChange = {
                    entry = it
                    message = ""
                },
                label = { Text(stringResource(R.string.entry_label)) },
                placeholder = {
                    Text(stringResource(R.string.entry_placeholder))
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !saving
            )

            Button(
                onClick = {
                    val text = entry.trim()

                    if (text.isBlank()) {
                        message = context.getString(R.string.error_empty_entry)
                    } else if (!saving) {
                        saving = true
                        message = ""

                        onSave(text) { success, error ->
                            saving = false

                            if (success) {
                                entry = ""
                                message = context.getString(R.string.save_success)
                            } else {
                                message = context.getString(
                                    R.string.save_failed,
                                    error ?: context.getString(R.string.error_retry)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving
            ) {
                Text(
                    if (saving) stringResource(R.string.saving_in_progress)
                    else stringResource(R.string.action_save)
                )
            }

            if (message.isNotBlank()) {
                Text(message)
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.entry_list_header, savedEntries.size),
                style = MaterialTheme.typography.titleMedium
            )

            if (savedEntries.isEmpty()) {
                Text(stringResource(R.string.empty_state))
            } else {
                savedEntries.forEachIndexed { index, item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("${index + 1}. ${item.rawText}")

                            Text(
                                text = java.text.SimpleDateFormat(
                                    "dd-MM-yyyy HH:mm",
                                    java.util.Locale.getDefault()
                                ).format(java.util.Date(item.createdAt)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
