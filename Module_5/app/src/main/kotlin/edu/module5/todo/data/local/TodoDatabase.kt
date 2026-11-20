package edu.module5.todo.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import edu.module5.todo.data.model.SeedImport
import edu.module5.todo.data.model.TaskRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM tasks ORDER BY id")
    fun observeTasks(): Flow<List<TaskRecord>>

    @Insert
    suspend fun insertTasks(tasks: List<TaskRecord>)

    @Update
    suspend fun updateTask(task: TaskRecord)

    @Delete
    suspend fun deleteTask(task: TaskRecord)

    @Query("SELECT EXISTS(SELECT 1 FROM seed_imports WHERE name = :name)")
    suspend fun hasImported(name: String): Boolean

    @Insert
    suspend fun markImported(seed: SeedImport)
}

@Database(entities = [TaskRecord::class, SeedImport::class], version = 1, exportSchema = false)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao

    companion object {
        private var database: TodoDatabase? = null

        @Synchronized
        fun get(context: Context): TodoDatabase =
            database ?: Room.databaseBuilder(
                context.applicationContext,
                TodoDatabase::class.java,
                "todolist.db",
            ).build().also { database = it }
    }
}
