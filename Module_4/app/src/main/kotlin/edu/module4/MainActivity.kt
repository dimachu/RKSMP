package edu.module4

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
    ModuleTask("Поиск репозиториев", "Поиск с отменой предыдущего запроса", 3) to
        Destination(edu.practice1.MainActivity::class.java, 0),
    ModuleTask("Социальная лента", "Посты и комментарии", 4) to
        Destination(edu.practice1.MainActivity::class.java, 1),
    ModuleTask("Foreground Service", "Счётчик с постоянным уведомлением", 5) to
        Destination(edu.practice2.MainActivity::class.java, 0),
    ModuleTask("Background Service", "Таймер и уведомление о завершении", 6) to
        Destination(edu.practice2.MainActivity::class.java, 1),
    ModuleTask("Bound Service", "Случайные числа до отвязки", 7) to
        Destination(edu.practice2.MainActivity::class.java, 2),
    ModuleTask("Обработка фотографии", "Сжатие, водяной знак и имитация загрузки", 8) to
        Destination(edu.practice3.MainActivity::class.java, 0),
    ModuleTask("Отчёт о погоде", "Параллельный прогноз по трём городам", 9) to
        Destination(edu.practice3.MainActivity::class.java, 1),
    ModuleTask("Местоположение", "Координаты и адрес", 10) to
        Destination(edu.practice4.MainActivity::class.java, 0),
    ModuleTask("Напоминание в 20:00", "Ежедневное уведомление", 11) to
        Destination(edu.practice5.MainActivity::class.java, 0),
    ModuleTask("Факты о животных", "Случайные факты через Flow", 12) to
        Destination(edu.practice6.MainActivity::class.java, 0),
    ModuleTask("Курс валюты", "Текущий курс через StateFlow", 13) to
        Destination(edu.practice6.MainActivity::class.java, 1),
    ModuleTask("Компас", "Азимут и направление по датчикам", 14) to
        Destination(edu.practice7.MainActivity::class.java, 0),
)

private val consoleTasks = listOf(
    ModuleTask(
        "Параллельная загрузка данных",
        "Три независимых JSON-источника",
        1,
        RunInstructions(
            "Выполните команды в PowerShell из папки Module_4 на компьютере.",
            ".\\gradlew.bat :console:sources -Psuccess\n.\\gradlew.bat :console:sources -Pfail",
            "Сначала программа выведет общий отчёт, затем покажет результат при сбое одного источника.",
        ),
    ),
    ModuleTask(
        "Поиск одинаковых JSON",
        "Группировка по SHA-256 с тайм-аутом",
        2,
        RunInstructions(
            "Положите JSON-файлы в папку C:\\AndroidTasks\\samples и запустите команду из Module_4.",
            ".\\gradlew.bat :console:duplicates -Pdirectory=C:\\AndroidTasks\\samples -Pseconds=10",
            "В терминале появятся группы совпавших файлов или сообщение о результате поиска.",
        ),
    ),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(0, 0),
            navigationBarStyle = SystemBarStyle.light(0, 0),
        )
        setContent {
            ModuleHome("Модуль 4", consoleTasks + destinations.keys) { task ->
                val destination = destinations.getValue(task)
                startActivity(
                    Intent(this, destination.activity)
                        .putExtra("edu.moduleui.INITIAL_TAB", destination.initialTab),
                )
            }
        }
    }
}
