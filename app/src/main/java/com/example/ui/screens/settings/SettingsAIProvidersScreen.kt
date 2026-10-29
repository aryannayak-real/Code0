package com.example.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AIProviderEntity
import com.example.ui.components.DetailScreenTopBar
import java.util.UUID

@Composable
fun SettingsAIProvidersScreen(
    providers: List<AIProviderEntity>,
    activeProvider: AIProviderEntity?,
    onSetActiveProvider: (String) -> Unit,
    onSaveProvider: (AIProviderEntity) -> Unit,
    onDeleteProvider: (String) -> Unit,
    onTestProvider: (AIProviderEntity, (String) -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingProvider by remember { mutableStateOf<AIProviderEntity?>(null) }
    var providerToDelete by remember { mutableStateOf<AIProviderEntity?>(null) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DetailScreenTopBar(title = "AI Providers", onBack = onBack)
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = "Configure API keys for real AI Chat and multi-file Agent mode. Keys are securely stored on-device in app preferences and never logged.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            items(providers) { provider ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = provider.isActive,
                            onClick = { onSetActiveProvider(provider.id) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = provider.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Model: ${provider.modelName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            val keyDisplay = if (provider.apiKey.isNotBlank()) {
                                "••••" + provider.apiKey.takeLast(4)
                            } else {
                                "No API key configured"
                            }
                            Text(
                                text = "Key: $keyDisplay",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (provider.apiKey.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }

                        Button(
                            onClick = {
                                editingProvider = provider
                                testResult = null
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Configure", fontSize = 11.sp)
                        }

                        if (providers.size > 1) {
                            IconButton(onClick = { providerToDelete = provider }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        editingProvider = AIProviderEntity(
                            id = UUID.randomUUID().toString(),
                            name = "OpenAI Compatible",
                            type = "CUSTOM",
                            apiKey = "",
                            modelName = "gpt-4o-mini",
                            endpointUrl = "https://api.openai.com/v1/chat/completions"
                        )
                        testResult = null
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_provider_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add AI Provider")
                }
            }
        }
    }

    // Configure Provider Modal Dialog
    if (editingProvider != null) {
        val target = editingProvider!!
        var name by remember { mutableStateOf(target.name) }
        var apiKey by remember { mutableStateOf(target.apiKey) }
        var modelName by remember { mutableStateOf(target.modelName) }
        var endpoint by remember { mutableStateOf(target.endpointUrl) }

        AlertDialog(
            onDismissRequest = {
                editingProvider = null
                testResult = null
            },
            title = { Text("Configure ${target.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Provider Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        placeholder = { Text("Enter secret key...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("provider_key_input")
                    )
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("Model Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (target.type != "GEMINI") {
                        OutlinedTextField(
                            value = endpoint,
                            onValueChange = { endpoint = it },
                            label = { Text("Endpoint URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (testResult != null) {
                        Text(
                            text = testResult!!,
                            fontSize = 11.sp,
                            color = if (testResult!!.startsWith("Connection successful")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            isTesting = true
                            val updated = target.copy(apiKey = apiKey.trim(), modelName = modelName.trim(), endpointUrl = endpoint.trim())
                            onTestProvider(updated) { res ->
                                testResult = res
                                isTesting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("test_provider_btn")
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text("Test Connection")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = target.copy(
                            name = name.trim(),
                            apiKey = apiKey.trim(),
                            modelName = modelName.trim(),
                            endpointUrl = endpoint.trim()
                        )
                        onSaveProvider(updated)
                        editingProvider = null
                        testResult = null
                    },
                    modifier = Modifier.testTag("save_provider_btn")
                ) {
                    Text("Save Provider")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editingProvider = null
                        testResult = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Provider Confirmation
    if (providerToDelete != null) {
        AlertDialog(
            onDismissRequest = { providerToDelete = null },
            title = { Text("Delete Provider?") },
            text = { Text("Are you sure you want to remove '${providerToDelete!!.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProvider(providerToDelete!!.id)
                        providerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { providerToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
