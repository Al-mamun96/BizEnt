
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
                                    e.localizedMessage ?: "অজানা সমস্যা হয়েছে"
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
                text = "আমার ব্যবসার হিসাব",
                style = MaterialTheme.typography.headlineSmall
            )

            Text("বাংলা অথবা Banglish-এ হিসাব লিখুন")

            OutlinedTextField(
                value = entry,
                onValueChange = {
                    entry = it
                    message = ""
                },
                label = { Text("হিসাবের বিবরণ") },
                placeholder = {
                    Text("যেমন: আজ ৫০০০ টাকার কাপড় কিনেছি")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !saving
            )

            Button(
                onClick = {
                    val text = entry.trim()

                    if (text.isBlank()) {
                        message = "আগে হিসাব লিখুন"
                    } else if (!saving) {
                        saving = true
                        message = ""

                        onSave(text) { success, error ->
                            saving = false

                            if (success) {
                                entry = ""
                                message = "হিসাব সফলভাবে সংরক্ষিত হয়েছে"
                            } else {
                                message = "সংরক্ষণ হয়নি: ${error ?: "আবার চেষ্টা করুন"}"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving
            ) {
                Text(
                    if (saving) "সংরক্ষণ হচ্ছে..."
                    else "হিসাব Save করুন"
                )
            }

            if (message.isNotBlank()) {
                Text(message)
            }

            HorizontalDivider()

            Text(
                text = "এন্ট্রির তালিকা (${savedEntries.size})",
                style = MaterialTheme.typography.titleMedium
            )

            if (savedEntries.isEmpty()) {
                Text("এখনো কোনো হিসাব এন্ট্রি করা হয়নি।")
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
