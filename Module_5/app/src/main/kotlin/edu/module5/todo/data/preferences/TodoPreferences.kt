package edu.module5.todo.data.preferences

import android.content.Context
import edu.module5.todo.domain.repository.TaskDisplayPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.todoSettings by preferencesDataStore(name = "todo_settings")
private val colorCompleted = booleanPreferencesKey("color_completed_tasks")

class TodoPreferences(context: Context) : TaskDisplayPreferences {
    private val store = context.applicationContext.todoSettings

    override val highlightsCompleted = store.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it[colorCompleted] ?: false }

    override suspend fun setHighlightsCompleted(enabled: Boolean) {
        store.edit { it[colorCompleted] = enabled }
    }
}
