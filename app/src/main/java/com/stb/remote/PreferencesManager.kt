package com.stb.remote

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("stb_remote_config", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BOT_TOKEN = "bot_token"
        private const val KEY_CHAT_ID = "chat_id"
    }

    fun saveConfig(botToken: String, chatId: String) {
        prefs.edit()
            .putString(KEY_BOT_TOKEN, botToken.trim())
            .putString(KEY_CHAT_ID, chatId.trim())
            .apply()
    }

    fun getBotToken(): String = prefs.getString(KEY_BOT_TOKEN, "").orEmpty()

    fun getChatId(): String = prefs.getString(KEY_CHAT_ID, "").orEmpty()

    fun isConfigured(): Boolean {
        return getBotToken().isNotEmpty() && getChatId().isNotEmpty()
    }
}
