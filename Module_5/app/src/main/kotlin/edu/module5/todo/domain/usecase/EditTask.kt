package edu.module5.todo.domain.usecase

import edu.module5.todo.domain.model.Task
import edu.module5.todo.domain.repository.Tasks

class EditTask(private val tasks: Tasks) {
    suspend fun save(task: Task) {
        val title = task.title.trim()
        require(title.isNotEmpty())
        tasks.save(task.copy(title = title, description = task.description.trim()))
    }

    suspend fun delete(task: Task) {
        tasks.delete(task)
    }
}
