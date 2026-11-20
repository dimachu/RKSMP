package edu.module5.todo

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import edu.module5.todo.data.local.TodoDatabase
import edu.module5.todo.data.repository.RoomTasks
import edu.module5.todo.domain.model.Task
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TodoRepositoryTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun importIsAtomicAndStaysCompleteAfterDeletingEverythingAndReopening() = runBlocking {
        val file = "todo-test-${UUID.randomUUID()}.db"
        suspend fun withDatabase(block: suspend (TodoDatabase) -> Unit) {
            val database = Room.databaseBuilder(context, TodoDatabase::class.java, file).build()
            try {
                block(database)
            } finally {
                database.close()
            }
        }

        try {
            withDatabase { database ->
                val tasks = RoomTasks(database) { "[broken" }
                assertThrows(IllegalArgumentException::class.java) {
                    runBlocking { tasks.importOnce() }
                }
                assertEquals(emptyList<Task>(), tasks.tasks.first())
            }
            withDatabase { database ->
                val tasks = RoomTasks(database) {
                    """[{"title":"Задача А","completed":true},{"title":"Задача Б"}]"""
                }
                coroutineScope {
                    val left = async { tasks.importOnce() }
                    val right = async { tasks.importOnce() }
                    left.await()
                    right.await()
                }
                val imported = tasks.tasks.first()
                assertEquals(listOf("Задача А", "Задача Б"), imported.map { it.title })
                assertEquals(listOf(true, false), imported.map { it.completed })

                tasks.save(Task(title = "Добавленная задача"))
                val added = tasks.tasks.first().single { it.title == "Добавленная задача" }
                tasks.save(added.copy(title = "Новый заголовок", completed = true))
                assertEquals(added.copy(title = "Новый заголовок", completed = true),
                    tasks.tasks.first().single { it.id == added.id })
                tasks.tasks.first().forEach { tasks.delete(it) }
                assertEquals(emptyList<Task>(), tasks.tasks.first())
            }
            withDatabase { database ->
                val reopened = RoomTasks(database) { error("Seed must not be read a second time") }
                reopened.importOnce()
                assertEquals(emptyList<Task>(), reopened.tasks.first())
            }
        } finally {
            context.deleteDatabase(file)
        }
        Unit
    }
}
