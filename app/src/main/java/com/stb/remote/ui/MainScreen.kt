package com.stb.remote.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stb.remote.PreferencesManager
import com.stb.remote.TelegramService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(preferencesManager: PreferencesManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var botToken by remember { mutableStateOf(preferencesManager.getBotToken()) }
    var chatId by remember { mutableStateOf(preferencesManager.getChatId()) }
    var manualLink by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val isConfigured = botToken.isNotBlank() && chatId.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("STB Remote Control", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Status Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isConfigured) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isConfigured) "Connected & Ready" else "Configuration Required",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isConfigured)
                                "Android Share & Magnet links will forward to STB"
                            else
                                "Enter your Telegram Bot Token & Chat ID below",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Quick Send Card
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Direct Send to STB", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Paste any magnet link, torrent URL, or YouTube link to start downloading immediately on your STB.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = manualLink,
                        onValueChange = { manualLink = it },
                        label = { Text("Paste Magnet / Link") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        trailingIcon = {
                            if (manualLink.isNotEmpty()) {
                                IconButton(onClick = { manualLink = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (manualLink.isBlank()) return@Button
                            isSending = true
                            scope.launch {
                                val result = TelegramService.sendMessage(botToken, chatId, manualLink.trim())
                                isSending = false
                                result.onSuccess {
                                    Toast.makeText(context, "🚀 Sent to STB!", Toast.LENGTH_SHORT).show()
                                    manualLink = ""
                                }.onFailure {
                                    Toast.makeText(context, "Error: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = isConfigured && manualLink.isNotBlank() && !isSending,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSending) "Sending..." else "Send to STB")
                    }
                }
            }

            // Quick Bot Commands
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Quick Bot Actions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    val commands = listOf(
                        "📊 Status" to "/status",
                        "⚡ Speeds" to "/speed",
                        "📦 Active" to "/list",
                        "🧹 Cleanup" to "/cleanup"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        commands.take(2).forEach { (label, cmd) ->
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        TelegramService.sendMessage(botToken, chatId, cmd)
                                        Toast.makeText(context, "Sent $cmd", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = isConfigured,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        commands.drop(2).forEach { (label, cmd) ->
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        TelegramService.sendMessage(botToken, chatId, cmd)
                                        Toast.makeText(context, "Sent $cmd", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = isConfigured,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }

            // Settings Card
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Telegram Bridge Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { botToken = it },
                        label = { Text("Bot Token (from @BotFather)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = chatId,
                        onValueChange = { chatId = it },
                        label = { Text("Telegram User ID / Chat ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            preferencesManager.saveConfig(botToken, chatId)
                            scope.launch {
                                isSending = true
                                val res = TelegramService.sendMessage(
                                    botToken,
                                    chatId,
                                    "📱 *STB Remote Android App* connected successfully!"
                                )
                                isSending = false
                                res.onSuccess {
                                    Toast.makeText(context, "✅ Settings saved & verified!", Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, "❌ Error: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Test Connection")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
