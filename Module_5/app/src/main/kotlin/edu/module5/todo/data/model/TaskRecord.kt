package edu.module5.todo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "tasks")
data class TaskRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val completed: Boolean,
)

@Entity(tableName = "seed_imports")
data class SeedImport(@PrimaryKey val name: String)

@Serializable
data class SeedTask(
    val title: String,
    val description: String = "",
    val completed: Boolean = false,
)
