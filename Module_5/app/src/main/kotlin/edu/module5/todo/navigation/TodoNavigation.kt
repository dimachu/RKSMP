package edu.module5.todo.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.module5.todo.domain.model.Task
import edu.module5.todo.presentation.ui.screen.TaskEditor
import edu.module5.todo.presentation.ui.screen.TaskListScreen
import edu.module5.todo.presentation.viewmodel.TodoModel

@Composable
fun TodoNavigation(onBack: () -> Unit, model: TodoModel) {
    val ready by model.ready.collectAsStateWithLifecycle()
    val tasks by model.tasks.collectAsStateWithLifecycle()
    val color by model.highlightsCompleted.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }

    BackHandler(enabled = editing != null) { editing = null }
    val currentId = editing
    val task = if (currentId == 0L) Task(title = "") else tasks.firstOrNull { it.id == currentId }

    if (task == null) {
        TaskListScreen(
            tasks = tasks,
            ready = ready,
            highlightCompleted = color,
            error = error,
            onBack = onBack,
            onSettingsChange = { model.highlight(it) },
            onAdd = { editing = 0L },
            onEdit = { editing = it.id },
            onToggle = { model.save(it.copy(completed = !it.completed)) },
            onRetry = { model.importTasks() },
        )
    } else {
        TaskEditor(
            task = task,
            error = error,
            onBack = { editing = null },
            onSave = { updated -> model.save(updated) { editing = null } },
            onDelete = { model.delete(task) { editing = null } },
        )
    }
}
