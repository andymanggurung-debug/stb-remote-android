package com.stb.remote.ui

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stb.remote.*
import kotlinx.coroutines.delay
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

    var dashboardState by remember { mutableStateOf<DashboardState?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    val isConfigured = botToken.isNotBlank() && chatId.isNotBlank()

    // Automatic polling loop every 8 seconds when configured
    LaunchedEffect(isConfigured, botToken, chatId) {
        if (!isConfigured) return@LaunchedEffect
        while (true) {
            val res = TelegramService.fetchLiveDashboard(botToken, chatId)
            res.onSuccess { dashboardState = it }
            delay(8000)
        }
    }

    val refreshDashboard: () -> Unit = {
        if (isConfigured && !isRefreshing) {
            isRefreshing = true
            scope.launch {
                val res = TelegramService.fetchLiveDashboard(botToken, chatId)
                isRefreshing = false
                res.onSuccess {
                    dashboardState = it
                }.onFailure {
                    Toast.makeText(context, "Refresh failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
                actions = {
                    if (isConfigured) {
                        val infiniteTransition = rememberInfiniteTransition(label = "spin")
                        val angle by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "spin"
                        )
                        IconButton(onClick = refreshDashboard) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = if (isRefreshing) Modifier.rotate(angle) else Modifier
                            )
                        }
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

            // 1. Live STB Conditions Card
            if (isConfigured) {
                LiveConditionsCard(
                    state = dashboardState,
                    isRefreshing = isRefreshing,
                    onRefresh = refreshDashboard
                )
            } else {
                // Warning Banner if not configured
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Configuration Required", fontWeight = FontWeight.Bold)
                            Text(
                                "Enter your Bot Token and Telegram Chat ID below to link with STB.",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 2. Active Transfers Section
            if (isConfigured) {
                ActiveTransfersCard(
                    transfers = dashboardState?.transfers ?: emptyList(),
                    isIdle = dashboardState?.isIdle ?: true,
                    onSendAction = { cmd ->
                        scope.launch {
                            TelegramService.sendMessage(botToken, chatId, cmd)
                            Toast.makeText(context, "Action sent: $cmd", Toast.LENGTH_SHORT).show()
                            delay(1000)
                            refreshDashboard()
                        }
                    }
                )
            }

            // 3. Quick Send Card
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Direct Send to STB", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Paste any magnet link, torrent URL, YouTube, Twitter/X, or manga query.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = manualLink,
                        onValueChange = { manualLink = it },
                        label = { Text("Paste Magnet / URL / Command") },
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
                                    delay(1500)
                                    refreshDashboard()
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

            // 4. Quick Actions
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Quick Commands", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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

            // 5. Telegram Bridge Settings
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
                                    refreshDashboard()
                                }.onFailure {
                                    Toast.makeText(context, "❌ Error: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Verify Connection")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LiveConditionsCard(
    state: DashboardState?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val cond = state?.condition

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (cond != null) Color(0xFF388E3C) else Color.Gray,
                                shape = RoundedCornerShape(5.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📡 STB Live Conditions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                if (state?.lastUpdatedText?.isNotEmpty() == true) {
                    Text(
                        text = "Updated: ${state.lastUpdatedText}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stat Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Temperature
                MetricBox(
                    modifier = Modifier.weight(1f),
                    icon = "🌡",
                    title = "Temp",
                    value = cond?.temp?.ifEmpty { "N/A" } ?: "N/A"
                )

                // Speeds
                MetricBox(
                    modifier = Modifier.weight(1f),
                    icon = "⚡",
                    title = "Download",
                    value = cond?.dlSpeed ?: "0.0B/s"
                )

                // Uptime
                MetricBox(
                    modifier = Modifier.weight(1f),
                    icon = "⏱",
                    title = "Uptime",
                    value = cond?.uptime?.ifEmpty { "N/A" } ?: "N/A"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // RAM Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🧠 RAM Memory", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (cond != null && cond.ramTotalMb > 0)
                            "${cond.ramUsedMb} / ${cond.ramTotalMb} MB (${(cond.ramPercent * 100).toInt()}%)"
                        else "N/A",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = cond?.ramPercent ?: 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }
    }
}

@Composable
fun MetricBox(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$icon $title", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun ActiveTransfersCard(
    transfers: List<ActiveTransfer>,
    isIdle: Boolean,
    onSendAction: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📦 Transfers & Progress",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (transfers.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${transfers.size} active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (transfers.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF388E3C))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "All transfers complete. STB is idle.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    transfers.forEach { item ->
                        TransferItemRow(item)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSendAction("/pause") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⏸ Pause All", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { onSendAction("/resume") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("▶️ Resume All", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { onSendAction("/cleanup") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🧹 Cleanup", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun TransferItemRow(item: ActiveTransfer) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.type.icon,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${item.progress}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = (item.progress / 100f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                shape = RoundedCornerShape(3.dp)
            )

            if (item.info.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.info,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
