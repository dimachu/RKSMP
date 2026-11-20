package edu.module5.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.module5.todo.data.local.TodoDatabase
import edu.module5.todo.data.preferences.TodoPreferences
import edu.module5.todo.data.repository.RoomTasks
import edu.module5.todo.presentation.viewmodel.TodoModel
import edu.module5.todo.navigation.TodoNavigation
import edu.moduleui.ModuleTheme

class MainActivity : ComponentActivity() {
    private val model by viewModels<TodoModel> {
        viewModelFactory {
            initializer {
                val tasks = RoomTasks(TodoDatabase.get(application)) {
                    application.assets.open("todo_seed.json").bufferedReader().use { it.readText() }
                }
                TodoModel(tasks, TodoPreferences(application))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bars = SystemBarStyle.light(0, 0)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        setContent {
            ModuleTheme { TodoNavigation(onBack = ::finish, model = model) }
        }
    }
}
