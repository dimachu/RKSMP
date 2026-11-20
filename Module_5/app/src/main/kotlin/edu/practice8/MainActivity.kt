package edu.practice8

import edu.moduleui.PracticeShell
import edu.moduleui.usePracticeSystemBars
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usePracticeSystemBars()
        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContent {
            PracticeShell("Модуль 5", onBack = ::finish) {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        var selectedTab by rememberSaveable { mutableIntStateOf(intent.getIntExtra("edu.moduleui.INITIAL_TAB", 0)) }
                        val tabs = listOf("Дневник", "Фото")
                        SingleChoiceSegmentedButtonRow(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                            tabs.forEachIndexed { index, title ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index, tabs.size),
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    icon = {},
                                ) { Text(title) }
                            }
                        }

                        Box(Modifier.weight(1f).widthIn(max = 560.dp).fillMaxSize()) {
                            if (selectedTab == 0) {
                                Diary()
                            } else {
                                Gallery()
                            }
                        }
                    }
                }
            }
        }
    }
}
