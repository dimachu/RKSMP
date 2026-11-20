package edu.module5.todo.domain.repository

import edu.module5.todo.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface Tasks {
    val tasks: Flow<List<Task>>
    suspend fun importOnce()
    suspend fun save(task: Task)
    suspend fun delete(task: Task)
}

interface TaskDisplayPreferences {
    val highlightsCompleted: Flow<Boolean>
    suspend fun setHighlightsCompleted(enabled: Boolean)
}
