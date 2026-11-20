package edu.practice11.nobel

import edu.moduleui.PracticeShell
import edu.moduleui.usePracticeSystemBars
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.practice11.nobel.data.Nobel
import coil.compose.AsyncImage
import edu.practice11.nobel.domain.Laureate
import edu.practice11.nobel.presentation.ScreenModel

class MainActivity : ComponentActivity() {
    private val model by viewModels<ScreenModel> {
        viewModelFactory { initializer { ScreenModel(Nobel()) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usePracticeSystemBars()
        setContent {
            PracticeShell("Модуль 6", onBack = ::finish) {
                Surface(Modifier.fillMaxSize()) {
                    NobelScreen(model)
                }
            }
        }
    }
}

@Composable
private fun NobelScreen(model: ScreenModel) {
    val listState = rememberLazyListState()
    val selected = model.selected
    val modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        when {
            selected != null -> LaureateDetails(selected, modifier) { model.selected = null }
            else -> Column(modifier.widthIn(max = 640.dp).fillMaxSize()) {
                LaureateList(model, listState, Modifier.weight(1f))
                Column(Modifier.padding(horizontal = 18.dp)) {
                    RequestFeedback(model)
                }
            }
        }
    }
}

@Composable
private fun RequestFeedback(model: ScreenModel) {
    if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (model.error.isNotEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                Text(model.error, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = model::retry) { Text("Повторить") }
            }
        }
    }
}

@Composable
private fun LaureateDetails(laureate: Laureate, modifier: Modifier, onBack: () -> Unit) {
    Column(modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 18.dp)) {
        TextButton(onClick = onBack) { Text("Назад") }
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(laureate.name, style = MaterialTheme.typography.headlineSmall)
                    PrizeLabel(laureate, prominent = true)
                    Text(
                        laureate.country ?: "Страна не указана / неприменима",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (laureate.portrait == null) {
                        Text(
                            "Фото отсутствует",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            laureate.portrait?.let { portrait ->
                item {
                    AsyncImage(
                        model = portrait,
                        contentDescription = "Портрет",
                        modifier = Modifier.size(180.dp).clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Мотивация", style = MaterialTheme.typography.titleMedium)
                    Text(laureate.motivation, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun LaureateFilters(model: ScreenModel) {
    val focus = LocalFocusManager.current
    val fontScale = LocalDensity.current.fontScale
    val selectCategory: (String) -> Unit = { category ->
        focus.clearFocus()
        model.category = category
        model.load()
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 336.dp && fontScale <= 1.15f) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        YearField(model, Modifier.weight(0.8f))
                        CategoryMenu(model.category, Modifier.weight(1.2f).padding(top = 8.dp), selectCategory)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        YearField(model, Modifier.fillMaxWidth())
                        CategoryMenu(model.category, Modifier.fillMaxWidth(), selectCategory)
                    }
                }
            }
            Button(
                onClick = {
                    focus.clearFocus()
                    model.load()
                },
                enabled = !model.busy,
                modifier = Modifier.align(Alignment.End),
            ) { Text("Применить") }
        }
    }
}

@Composable
private fun YearField(model: ScreenModel, modifier: Modifier) {
    OutlinedTextField(
        value = model.year,
        onValueChange = { model.year = it },
        modifier = modifier,
        label = { Text("Год") },
        placeholder = { Text("Любой") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun LaureateList(model: ScreenModel, listState: LazyListState, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Публичный Nobel", style = MaterialTheme.typography.headlineSmall)
                LaureateFilters(model)
            }
        }
        if (!model.busy && model.error.isEmpty() && model.laureates.isEmpty()) {
            item {
                Text(
                    "Нет результатов",
                    Modifier.padding(vertical = 14.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(model.laureates, key = { "${it.id}-${it.year}-${it.category}" }) { laureate ->
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clickable { model.detail(laureate) }
                    .padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(laureate.name, style = MaterialTheme.typography.titleMedium)
                PrizeLabel(laureate, prominent = false)
                Text(
                    laureate.motivation.take(100),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun PrizeLabel(laureate: Laureate, prominent: Boolean) {
    Text(
        "${laureate.year} · ${laureate.category}",
        style = if (prominent) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

private val categories = listOf(
    "" to "Все",
    "phy" to "physics",
    "che" to "chemistry",
    "med" to "medicine",
    "lit" to "literature",
    "pea" to "peace",
    "eco" to "economics",
)

@Composable
private fun CategoryMenu(category: String, modifier: Modifier, onSelect: (String) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val selectedLabel = categories.firstOrNull { it.first == category }?.second ?: category
    Box(modifier) {
        OutlinedButton(
            onClick = { menuOpen = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = MaterialTheme.shapes.extraSmall,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Категория: $selectedLabel", Modifier.weight(1f))
                Text("▾")
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            categories.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        menuOpen = false
                        onSelect(code)
                    },
                )
            }
        }
    }
}
