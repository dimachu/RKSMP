package edu.module5.todo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.module5.todo.domain.repository.TaskDisplayPreferences
import edu.module5.todo.domain.repository.Tasks
import edu.module5.todo.domain.model.Task
import edu.module5.todo.domain.usecase.EditTask
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoModel(
    private val repository: Tasks,
    private val preferences: TaskDisplayPreferences,
) : ViewModel() {
    private val editTask = EditTask(repository)
    private val mutableReady = MutableStateFlow(false)
    private val mutableError = MutableStateFlow<String?>(null)
    val ready = mutableReady.asStateFlow()
    val error = mutableError.asStateFlow()
    val tasks = repository.tasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val highlightsCompleted = preferences.highlightsCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        importTasks()
    }

    fun importTasks() = perform("Не удалось загрузить начальные задачи") {
        repository.importOnce()
        mutableReady.value = true
    }

    fun highlight(enabled: Boolean) = perform("Не удалось сохранить настройку") {
        preferences.setHighlightsCompleted(enabled)
    }

    fun save(task: Task, onSuccess: () -> Unit = {}) = perform("Не удалось сохранить задачу") {
        editTask.save(task)
        onSuccess()
    }

    fun delete(task: Task, onSuccess: () -> Unit) = perform("Не удалось удалить задачу") {
        editTask.delete(task)
        onSuccess()
    }

    private fun perform(errorMessage: String, action: suspend () -> Unit) = viewModelScope.launch {
        mutableError.value = null
        try {
            action()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            mutableError.value = errorMessage
        }
    }
}
