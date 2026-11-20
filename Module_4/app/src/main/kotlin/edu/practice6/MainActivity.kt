package edu.practice6

import edu.moduleui.PracticeShell
import edu.moduleui.usePracticeSystemBars
import edu.moduleui.PracticeHeading
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usePracticeSystemBars()
        setContent {
            PracticeShell("Модуль 4", onBack = ::finish) {
                Surface(Modifier.fillMaxSize()) {
                    FlowPractice(intent.getIntExtra("edu.moduleui.INITIAL_TAB", 0))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlowPractice(initialTab: Int, model: FactsModel = viewModel()) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }

    Box(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 520.dp).fillMaxSize().padding(horizontal = 16.dp)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Факты") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Курс") })
            }
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (selectedTab == 0) AnimalFactsScreen(model) else ExchangeRateScreen(model)
            }
        }
    }
}

@Composable
private fun AnimalFactsScreen(model: FactsModel) {
    PracticeHeading("Факты о животных")
    Button(
        onClick = model::load,
        enabled = !model.loading,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Новый факт!") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.heightIn(min = 104.dp).padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedVisibility(
                visible = model.fact.isNotEmpty() && !model.loading,
                exit = ExitTransition.None,
            ) {
                Text(
                    model.fact,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (model.loading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text("Загружаем факт", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (model.fact.isEmpty()) {
                Text("Нажмите «Новый факт!», чтобы узнать факт о животных.")
            }
        }
    }
}

@Composable
private fun ExchangeRateScreen(model: FactsModel) {
    val rate by model.rate.collectAsState()
    val direction = model.previous?.let { rate.compareTo(it) } ?: 0
    val (arrow, color) = when {
        direction > 0 -> "↑" to MaterialTheme.colorScheme.tertiary
        direction < 0 -> "↓" to MaterialTheme.colorScheme.error
        else -> "—" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Курс USD/RUB", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "%.2f ₽".format(rate),
                    style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(arrow, style = MaterialTheme.typography.headlineMedium, color = color)
            }
            Text("Имитационный курс", style = MaterialTheme.typography.bodyMedium)
        }
    }
    Button(onClick = model::update, modifier = Modifier.fillMaxWidth()) {
        Text("Обновить сейчас")
    }
}
