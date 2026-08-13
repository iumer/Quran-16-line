package com.quran16line.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.quran16line.app.ui.reader.ReaderScreen
import com.quran16line.app.ui.theme.QuranTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        setContent {
            QuranTheme {
                ReaderScreen()
            }
        }
    }
}
