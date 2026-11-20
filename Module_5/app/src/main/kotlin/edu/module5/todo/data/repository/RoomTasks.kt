package edu.module5.todo.data.repository

import androidx.room.withTransaction
import edu.module5.todo.data.local.TodoDatabase
import edu.module5.todo.data.model.SeedImport
import edu.module5.todo.data.model.SeedTask
import edu.module5.todo.data.model.TaskRecord
import edu.module5.todo.domain.model.Task
import edu.module5.todo.domain.repository.Tasks
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class RoomTasks(
    private val database: TodoDatabase,
    private val readSeed: () -> String,
) : Tasks {
    private val dao = database.todoDao()
    override val tasks = dao.observeTasks().map { records ->
        records.map { Task(it.id, it.title, it.description, it.completed) }
    }

    override suspend fun importOnce() {
        database.withTransaction {
            val seed = SeedImport("todo_seed")
            if (dao.hasImported(seed.name)) return@withTransaction

            val records = Json.decodeFromString<List<SeedTask>>(readSeed()).map { task ->
                require(task.title.isNotBlank()) { "Заголовок начальной задачи пуст" }
                TaskRecord(title = task.title, description = task.description, completed = task.completed)
            }
            dao.insertTasks(records)
            dao.markImported(seed)
        }
    }

    override suspend fun save(task: Task) {
        val record = TaskRecord(task.id, task.title, task.description, task.completed)
        if (task.id == 0L) dao.insertTasks(listOf(record)) else dao.updateTask(record)
    }

    override suspend fun delete(task: Task) {
        dao.deleteTask(TaskRecord(task.id, task.title, task.description, task.completed))
    }
}
