package edu.moduleui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

data class ModuleTask(
    val title: String,
    val description: String,
    val number: Int? = null,
    val instructions: RunInstructions? = null,
)

data class RunInstructions(val setup: String, val commands: String, val result: String)

private val moduleColors = lightColorScheme(
    primary = Color(0xFF34558B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E4FA),
    onPrimaryContainer = Color(0xFF243B70),
    secondary = Color(0xFF50627E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE5F4),
    onSecondaryContainer = Color(0xFF263B58),
    tertiary = Color(0xFF256751),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFEDE3),
    onTertiaryContainer = Color(0xFF143E35),
    background = Color(0xFFF0F3FA),
    onBackground = Color(0xFF17263C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17263C),
    surfaceVariant = Color(0xFFE1E8F3),
    onSurfaceVariant = Color(0xFF43536B),
    surfaceContainer = Color(0xFFE8EEF8),
    surfaceContainerLow = Color(0xFFF5F7FC),
    surfaceContainerLowest = Color.White,
    surfaceContainerHigh = Color(0xFFE2EAF7),
    surfaceContainerHighest = Color(0xFFDCE5F3),
    inverseSurface = Color(0xFF243B70),
    inverseOnSurface = Color(0xFFF0F3FA),
    inversePrimary = Color(0xFFACC7F6),
    outline = Color(0xFF65758A),
    outlineVariant = Color(0xFFCBD7E9),
    error = Color(0xFF9A303B),
    onError = Color.White,
    errorContainer = Color(0xFFFCE2E5),
    onErrorContainer = Color(0xFF681D27),
)

private val baseType = Typography()
private val moduleType = baseType.copy(
    headlineMedium = baseType.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = baseType.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = baseType.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = baseType.titleMedium.copy(fontWeight = FontWeight.SemiBold),
)
private val moduleShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

@Composable
fun ModuleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = moduleColors, typography = moduleType, shapes = moduleShapes, content = content)
}

fun ComponentActivity.usePracticeSystemBars() {
    val bars = SystemBarStyle.light(0, 0)
    enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
}

@Composable
fun PracticeHeading(title: String) {
    Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
}

@Composable
fun PracticeShell(moduleTitle: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    ModuleTheme {
        Scaffold(
            topBar = { ModulePracticeBar(moduleTitle, onBack, backLabel = "К заданиям") },
        ) { insets ->
            Box(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
                content()
            }
        }
    }
}

@Composable
fun ModuleHome(title: String, tasks: List<ModuleTask>, onOpen: (ModuleTask) -> Unit) {
    var selectedNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    val selected = tasks.firstOrNull { it.number == selectedNumber && it.instructions != null }
    val stateHolder = rememberSaveableStateHolder()
    BackHandler(enabled = selected != null) { selectedNumber = null }
    ModuleTheme {
        if (selected != null) {
            InstructionsScreen(selected.title, checkNotNull(selected.instructions)) { selectedNumber = null }
        } else {
            stateHolder.SaveableStateProvider("catalog") {
                TaskCatalog(title, tasks) { task ->
                    if (task.instructions != null) selectedNumber = task.number else onOpen(task)
                }
            }
        }
    }
}

@Composable
private fun TaskCatalog(title: String, tasks: List<ModuleTask>, onOpen: (ModuleTask) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 288.dp),
                modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
                contentPadding = PaddingValues(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CatalogHeading(title)
                }
                items(tasks, key = { it.number?.toString() ?: it.title }) { task ->
                    TaskEntry(task = task, onClick = { onOpen(task) })
                }
            }
        }
    }
}

@Composable
private fun CatalogHeading(title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 17.dp)
                .semantics(mergeDescendants = true) { heading() },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.inversePrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Практические задания",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TaskEntry(task: ModuleTask, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 76.dp)
            .semantics { role = Role.Button },
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            task.number?.let { number ->
                Surface(
                    modifier = Modifier.clearAndSetSemantics { contentDescription = "Задание $number" },
                    shape = MaterialTheme.shapes.extraSmall,
                    color = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                ) {
                    Text(
                        text = number.toString(),
                        modifier = Modifier.widthIn(min = 36.dp).padding(horizontal = 6.dp, vertical = 5.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        // The semantics label already supplies the number with its meaning.
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            DirectionArrow(forward = true, color = colors.primary, modifier = Modifier.size(19.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulePracticeBar(
    title: String,
    onBack: () -> Unit,
    backLabel: String = "Назад",
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    TopAppBar(
        title = {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = backLabel },
            ) {
                DirectionArrow(forward = false, color = colors.primary, modifier = Modifier.size(24.dp))
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.background,
            titleContentColor = colors.onBackground,
        ),
    )
}

@Composable
private fun InstructionsScreen(title: String, instructions: RunInstructions, onBack: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by rememberSaveable { mutableStateOf(false) }
    Scaffold(topBar = { ModulePracticeBar(title, onBack) }) { insets ->
        Box(
            Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(instructions.setup, style = MaterialTheme.typography.bodyLarge)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Text(
                        instructions.commands,
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    )
                }
                OutlinedButton(onClick = {
                    clipboard.setText(AnnotatedString(instructions.commands))
                    copied = true
                }) { Text(if (copied) "Команды скопированы" else "Скопировать команды") }
                Text(instructions.result, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DirectionArrow(forward: Boolean, color: Color, modifier: Modifier = Modifier) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pointsRight = forward != isRtl
    Canvas(modifier) {
        val centerY = size.height / 2
        val tailX = size.width * if (pointsRight) 0.26f else 0.74f
        val tipX = size.width - tailX
        val wingX = size.width * if (pointsRight) 0.49f else 0.51f
        val stroke = 1.8.dp.toPx()
        val tip = Offset(tipX, centerY)
        drawLine(color, Offset(tailX, centerY), tip, stroke, StrokeCap.Round)
        drawLine(color, Offset(wingX, size.height * 0.26f), tip, stroke, StrokeCap.Round)
        drawLine(color, Offset(wingX, size.height * 0.74f), tip, stroke, StrokeCap.Round)
    }
}
