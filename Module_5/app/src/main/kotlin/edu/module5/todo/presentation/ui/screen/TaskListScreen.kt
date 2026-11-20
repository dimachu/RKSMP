package edu.module5.todo.presentation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import edu.module5.todo.domain.model.Task
import edu.module5.todo.presentation.ui.component.TaskRow
import edu.moduleui.ModulePracticeBar

@Composable
fun TaskListScreen(
    tasks: List<Task>,
    ready: Boolean,
    highlightCompleted: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSettingsChange: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Task) -> Unit,
    onToggle: (Task) -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            ModulePracticeBar(title = "Список задач", onBack = onBack) {
                Row(
                    modifier = Modifier.padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Цвет завершенных",
                        modifier = Modifier.widthIn(max = 92.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Switch(
                        checked = highlightCompleted,
                        onCheckedChange = onSettingsChange,
                        modifier = Modifier.semantics { contentDescription = "Цвет завершенных" },
                    )
                }
            }
        },
        floatingActionButton = {
            if (ready) ExtendedFloatingActionButton(onClick = onAdd) { Text("Добавить задачу") }
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (error != null) item {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    if (!ready) Button(onClick = onRetry) { Text("Повторить") }
                }
                if (!ready && error == null) item { CircularProgressIndicator() }
                if (ready && tasks.isEmpty()) item { Text("Задач пока нет", Modifier.padding(vertical = 16.dp)) }
                items(tasks, key = { it.id }) { task ->
                    TaskRow(task, highlightCompleted, { onEdit(task) }, { onToggle(task) })
                }
                item { Box(Modifier.padding(bottom = 80.dp)) }
            }
        }
    }
}
