package edu.practice9

import edu.moduleui.PracticeShell
import edu.moduleui.usePracticeSystemBars
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usePracticeSystemBars()
        setContent {
            PracticeShell("Модуль 5", onBack = ::finish) {
                Surface(Modifier.fillMaxSize()) {
                    BoxWithSafeInsets()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun BoxWithSafeInsets() {
    androidx.compose.foundation.layout.Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
        Gallery()
    }
}
