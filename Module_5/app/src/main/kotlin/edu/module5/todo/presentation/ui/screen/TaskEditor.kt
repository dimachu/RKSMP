package edu.module5.todo.presentation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.module5.todo.domain.model.Task
import edu.moduleui.ModulePracticeBar

@Composable
fun TaskEditor(
    task: Task,
    error: String?,
    onBack: () -> Unit,
    onSave: (Task) -> Unit,
    onDelete: () -> Unit,
) {
    var title by rememberSaveable(task.id) { mutableStateOf(task.title) }
    var description by rememberSaveable(task.id) { mutableStateOf(task.description) }
    Scaffold(
        topBar = {
            ModulePracticeBar(
                title = if (task.id == 0L) "Новая задача" else "Редактирование",
                onBack = onBack,
            )
        },
    ) { insets ->
        Box(
            Modifier.fillMaxSize().padding(insets).imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = title.isNotBlank(),
                    onClick = { onSave(task.copy(title = title, description = description)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Сохранить") }
                if (task.id != 0L) {
                    TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                    }
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
