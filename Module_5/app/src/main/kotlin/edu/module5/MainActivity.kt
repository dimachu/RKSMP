package edu.module5

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import edu.moduleui.ModuleHome
import edu.moduleui.ModuleTask

private data class Destination(val activity: Class<out ComponentActivity>, val initialTab: Int)

private val destinations = linkedMapOf(
    ModuleTask("Дневник", "Записи во внутреннем хранилище", 1) to
        Destination(edu.practice8.MainActivity::class.java, 0),
    ModuleTask("Личная галерея", "Снимки в папке приложения", 2) to
        Destination(edu.practice8.MainActivity::class.java, 1),
    ModuleTask("Расширенная галерея", "Экспорт снимка из сетки и просмотра", 3) to
        Destination(edu.practice9.MainActivity::class.java, 0),
    ModuleTask("Список задач", "Задачи и настройка цвета завершённых", 4) to
        Destination(edu.module5.todo.MainActivity::class.java, 0),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(0, 0),
            navigationBarStyle = SystemBarStyle.light(0, 0),
        )
        setContent {
            ModuleHome("Модуль 5", destinations.keys.toList()) { task ->
                val destination = destinations.getValue(task)
                startActivity(
                    Intent(this, destination.activity)
                        .putExtra("edu.moduleui.INITIAL_TAB", destination.initialTab),
                )
            }
        }
    }
}
