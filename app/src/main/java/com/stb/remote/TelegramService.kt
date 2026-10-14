package com.stb.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class StbCondition(
    val botName: String = "STB",
    val uptime: String = "",
    val temp: String = "",
    val ramUsedMb: Int = 0,
    val ramTotalMb: Int = 0,
    val ramPercent: Float = 0f,
    val dlSpeed: String = "0.0B/s",
    val ulSpeed: String = "0.0B/s"
)

enum class TransferType(val label: String, val icon: String) {
    TORRENT("Torrent", "🧲"),
    MEDIA("Media", "🎬"),
    UPLOAD("Upload", "☁️"),
    AUDIO("Audio", "🎵")
}

data class ActiveTransfer(
    val type: TransferType,
    val name: String,
    val progress: Int,
    val info: String
)

data class DashboardState(
    val condition: StbCondition? = null,
    val transfers: List<ActiveTransfer> = emptyList(),
    val isIdle: Boolean = true,
    val lastUpdatedText: String = "",
    val rawText: String = ""
)

object TelegramService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(botToken: String, chatId: String, message: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (botToken.isBlank() || chatId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Bot Token and Chat ID must not be empty"))
            }

            val url = "https://api.telegram.org/bot$botToken/sendMessage"
            val formBody = FormBody.Builder()
                .add("chat_id", chatId)
                .add("text", message)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful && body.contains("\"ok\":true")) {
                        Result.success("Sent successfully")
                    } else {
                        Result.failure(IOException("Telegram Error (${response.code}): $body"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun parseDashboardText(text: String): DashboardState {
        if (text.isBlank()) return DashboardState()

        val tempMatch = Regex("""🌡\s*([\d.]+°C)""").find(text)
        val uptimeMatch = Regex("""⏱\s*([^|\n]+)""").find(text)
        val ramMatch = Regex("""RAM:\s*(\d+)/(\d+)\s*MB""").find(text)
        val netMatch = Regex("""Torrent\s*↓(\S+)\s*↑(\S+)""").find(text)

        val ramUsed = ramMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        val ramTotal = ramMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
        val ramPct = if (ramTotal > 0) (ramUsed.toFloat() / ramTotal.toFloat()).coerceIn(0f, 1f) else 0f

        val condition = StbCondition(
            uptime = uptimeMatch?.groupValues?.getOrNull(1)?.trim().orEmpty(),
            temp = tempMatch?.groupValues?.getOrNull(1)?.trim().orEmpty(),
            ramUsedMb = ramUsed,
            ramTotalMb = ramTotal,
            ramPercent = ramPct,
            dlSpeed = netMatch?.groupValues?.getOrNull(1)?.trim().orEmpty().ifEmpty { "0.0B/s" },
            ulSpeed = netMatch?.groupValues?.getOrNull(2)?.trim().orEmpty().ifEmpty { "0.0B/s" }
        )

        val transfers = mutableListOf<ActiveTransfer>()
        val transferRegex = Regex("""(☁️|🎬|🎵|🧲)\s*[^:\n]+:\s*([^\n]+)\n[█░]*\s*(\d+)%\s*([^\n]*)""")
        for (m in transferRegex.findAll(text)) {
            val icon = m.groupValues[1]
            val name = m.groupValues[2].trim()
            val pct = m.groupValues[3].toIntOrNull() ?: 0
            val info = m.groupValues[4].trim()

            val type = when (icon) {
                "☁️" -> TransferType.UPLOAD
                "🎬" -> TransferType.MEDIA
                "🎵" -> TransferType.AUDIO
                else -> TransferType.TORRENT
            }

            transfers.add(ActiveTransfer(type, name, pct, info))
        }

        val isIdle = transfers.isEmpty() && (text.contains("Idle") || text.contains("No active"))
        return DashboardState(condition = condition, transfers = transfers, isIdle = isIdle, rawText = text)
    }

    suspend fun fetchLiveDashboard(botToken: String, chatId: String): Result<DashboardState> =
        withContext(Dispatchers.IO) {
            if (botToken.isBlank() || chatId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Bot Token and Chat ID required"))
            }

            val url = "https://api.telegram.org/bot$botToken/getChat?chat_id=$chatId"
            val request = Request.Builder().url(url).get().build()

            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful || !body.contains("\"ok\":true")) {
                        return@withContext Result.failure(IOException("HTTP Error ${response.code}: $body"))
                    }

                    val json = JSONObject(body)
                    val result = json.optJSONObject("result")
                    val pinned = result?.optJSONObject("pinned_message")
                    val text = pinned?.optString("text").orEmpty()
                    val editDate = pinned?.optLong("edit_date") ?: 0L
                    val date = pinned?.optLong("date") ?: 0L
                    val effectiveDate = if (editDate > 0L) editDate else date

                    val state = parseDashboardText(text)
                    val formattedTime = if (effectiveDate > 0) {
                        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        sdf.format(Date(effectiveDate * 1000L))
                    } else ""

                    Result.success(state.copy(lastUpdatedText = formattedTime))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
