package com.stb.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.stb.remote.ui.MainScreen
import com.stb.remote.ui.theme.STBRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferencesManager = PreferencesManager(this)

        setContent {
            STBRemoteTheme {
                MainScreen(preferencesManager = preferencesManager)
            }
        }
    }
}
