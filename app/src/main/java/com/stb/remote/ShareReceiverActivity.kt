package com.stb.remote

import androidx.activity.ComponentActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = PreferencesManager(this)
        if (!prefs.isConfigured()) {
            Toast.makeText(this, "Please configure STB Bot Token and Chat ID first!", Toast.LENGTH_LONG).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        val incomingContent = extractContent(intent)
        if (incomingContent.isNullOrBlank()) {
            Toast.makeText(this, "No valid link or magnet found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Send to STB Telegram Bot in the background
        lifecycleScope.launch {
            val result = TelegramService.sendMessage(
                prefs.getBotToken(),
                prefs.getChatId(),
                incomingContent
            )

            result.onSuccess {
                val preview = incomingContent.take(35)
                Toast.makeText(
                    this@ShareReceiverActivity,
                    "🚀 Sent to STB:\n$preview...",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { err ->
                Toast.makeText(
                    this@ShareReceiverActivity,
                    "❌ Failed to send: ${err.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }

            finish()
        }
    }

    private fun extractContent(intent: Intent): String? {
        val action = intent.action

        // 1. Magnet link clicked in browser (ACTION_VIEW)
        if (Intent.ACTION_VIEW == action && intent.data != null) {
            val dataStr = intent.data.toString()
            if (dataStr.startsWith("magnet:", ignoreCase = true)) {
                return dataStr
            }
        }

        // 2. Text / URL shared from another app (ACTION_SEND)
        if (Intent.ACTION_SEND == action && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null

            // Look for magnet link inside text
            val magnetMatcher = Pattern.compile("magnet:\\?xt=urn:[a-zA-Z0-9]+[^\\s]*").matcher(sharedText)
            if (magnetMatcher.find()) {
                return magnetMatcher.group()
            }

            // Look for web URL (YouTube, Torrent, etc.)
            val urlMatcher = Pattern.compile("https?://[^\\s]+").matcher(sharedText)
            if (urlMatcher.find()) {
                return urlMatcher.group()
            }

            // Fallback: return trimmed text
            return sharedText.trim()
        }

        return null
    }
}
