package edu.module6

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import edu.moduleui.ModuleHome
import edu.moduleui.ModuleTask
import edu.moduleui.RunInstructions

private data class Destination(val activity: Class<out ComponentActivity>, val initialTab: Int)

private val destinations = linkedMapOf(
    ModuleTask("Фотокаталог Picsum", "Просмотр и сохранение фотографии", 1) to
        Destination(edu.practice11.photos.MainActivity::class.java, 0),
    ModuleTask("Публичный Nobel", "Нобелевские премии из открытого API", 2) to
        Destination(edu.practice11.nobel.MainActivity::class.java, 0),
    ModuleTask("Вход через DummyJSON", "Учебная авторизация и профиль", 3) to
        Destination(edu.practice11.auth.MainActivity::class.java, 0),
    ModuleTask("Собственный Nobel API", "Премии и избранное через учебный сервер", 6) to
        Destination(edu.practice13.MainActivity::class.java, 0),
    ModuleTask("Пульс по Bluetooth", "Измерения с датчика BLE", 7) to
        Destination(edu.practice14.MainActivity::class.java, 0),
)

private val serverTasks = listOf(
    ModuleTask(
        "Локальный сервер Nobel",
        "Ktor с учебными данными из JSON",
        4,
        RunInstructions(
            "Откройте Module_6 и выполните команды в PowerShell 7 на компьютере.",
            "\$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))\n" +
                ".\\gradlew.bat :serverLocal:run",
            "По умолчанию сервер доступен по адресу http://localhost:8081. " +
                "В Android Studio задача запускается из Gradle → serverLocal → application → run.",
        ),
    ),
    ModuleTask(
        "Сервер Nobel с Neon",
        "Ktor с удалённой базой и избранным",
        5,
        RunInstructions(
            "Откройте Module_6 на компьютере. В первый раз выполните Configure и вставьте " +
                "строку PostgreSQL из Neon в закрытый запрос. Затем выполните Run.",
            ".\\neon.ps1 -Action Configure\n.\\neon.ps1 -Action Run",
            "При сохранённой конфигурации достаточно Run. " +
                "Документация сервера доступна по адресу http://localhost:8080/docs.",
        ),
    ),
)

private val tasks = (destinations.keys + serverTasks).sortedBy { it.number }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(0, 0),
            navigationBarStyle = SystemBarStyle.light(0, 0),
        )
        setContent {
            ModuleHome("Модуль 6", tasks) { task ->
                val destination = destinations.getValue(task)
                startActivity(
                    Intent(this, destination.activity)
                        .putExtra("edu.moduleui.INITIAL_TAB", destination.initialTab),
                )
            }
        }
    }
}
