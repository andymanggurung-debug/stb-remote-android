package com.stb.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

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
}
