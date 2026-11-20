package edu.practice5

import edu.moduleui.PracticeShell
import edu.moduleui.usePracticeSystemBars
import edu.moduleui.PracticeHeading
import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.ZonedDateTime

fun notification(context: Context, text: String, ongoing: Boolean = false): Notification {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
        NotificationChannel("events", "События", NotificationManager.IMPORTANCE_DEFAULT),
    )
    return Notification.Builder(context, "events")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(text)
        .setOngoing(ongoing)
        .build()
}

object Reminder {
    fun prefs(context: Context) = context.getSharedPreferences("reminder", Context.MODE_PRIVATE)

    fun intent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        10,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun schedule(context: Context): Boolean {
        val manager = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= 31 && !manager.canScheduleExactAlarms()) return false

        val triggerTime = nextReminder(ZonedDateTime.now()).toInstant().toEpochMilli()
        return try {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, intent(context))
            true
        } catch (exception: SecurityException) {
            false
        }
    }

    fun disable(context: Context) {
        prefs(context).edit().putBoolean("enabled", false).apply()
        context.getSystemService(AlarmManager::class.java).cancel(intent(context))
    }

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean("enabled", false)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Reminder.isEnabled(context)) return

        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.areNotificationsEnabled()) {
            manager.notify(10, notification(context, "Время принять таблетку!"))
        }
        if (!Reminder.schedule(context)) Reminder.disable(context)
    }
}

class RestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Reminder.isEnabled(context) && !Reminder.schedule(context)) {
            Reminder.disable(context)
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usePracticeSystemBars()
        setContent {
            PracticeShell("Модуль 4", onBack = ::finish) {
                Surface(Modifier.fillMaxSize()) {
                    ReminderScreen()
                }
            }
        }
    }
}

@Composable
private fun ReminderScreen() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(Reminder.isEnabled(context)) }
    var message by remember { mutableStateOf("") }
    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) message = "Разрешите уведомления для напоминания"
    }

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enabled = Reminder.isEnabled(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val toggleReminder = {
        when {
            enabled -> {
                Reminder.disable(context)
                enabled = false
            }
            needsNotificationPermission(context) -> {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            needsExactAlarmAccess(context) -> {
                message = "Разрешите точные напоминания в настройках и нажмите включение ещё раз"
                openExactAlarmSettings(context)
            }
            else -> {
                enabled = Reminder.schedule(context)
                Reminder.prefs(context).edit().putBoolean("enabled", enabled).apply()
                message = if (enabled) "" else "Точный будильник недоступен"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PracticeHeading("Напоминание о таблетке")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("20:00", style = MaterialTheme.typography.displayLarge)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val colors = MaterialTheme.colorScheme
                            Box(
                                Modifier.size(10.dp).background(
                                    if (enabled) colors.tertiary else colors.outline,
                                    CircleShape,
                                ),
                            )
                            Text(
                                if (enabled) "Напоминание включено" else "Напоминание выключено",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        if (enabled) {
                            val now = ZonedDateTime.now()
                            val day = if (nextReminder(now).toLocalDate() == now.toLocalDate()) "сегодня" else "завтра"
                            Text("Следующее напоминание: $day в 20:00", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (enabled) {
                            OutlinedButton(onClick = toggleReminder, modifier = Modifier.fillMaxWidth()) {
                                Text("Выключить напоминание", textAlign = TextAlign.Center)
                            }
                        } else {
                            Button(onClick = toggleReminder, modifier = Modifier.fillMaxWidth()) {
                                Text("Включить напоминание", textAlign = TextAlign.Center)
                            }
                        }
                        if (message.isNotBlank()) {
                            Text(message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= 33 &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

private fun needsExactAlarmAccess(context: Context): Boolean =
    Build.VERSION.SDK_INT >= 31 &&
        !context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

private fun openExactAlarmSettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}"),
        ),
    )
}
