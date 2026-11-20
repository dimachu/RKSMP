package edu.practice2

import edu.moduleui.PracticeShell
import edu.moduleui.usePracticeSystemBars
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import edu.module4.notificationAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun notification(
    context: Context,
    text: String,
    ongoing: Boolean = false,
    body: String? = null,
): Notification {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
        NotificationChannel("events", "События", NotificationManager.IMPORTANCE_DEFAULT),
    )
    val builder = Notification.Builder(context, "events")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(text)
        .setContentText(body)
        .setOnlyAlertOnce(ongoing)
        .setOngoing(ongoing)

    if (ongoing && Build.VERSION.SDK_INT >= 31) {
        builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
    }
    return builder.build()
}

object CounterState {
    val seconds = MutableStateFlow(0)
}

class CounterService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var counterJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (counterJob?.isActive != true) {
            CounterState.seconds.value = 0
            startForeground(1, notification(this, "Прошло 0 секунд", ongoing = true))

            val manager = getSystemService(NotificationManager::class.java)
            counterJob = scope.launch {
                while (isActive) {
                    delay(1_000)
                    val seconds = CounterState.seconds.value + 1
                    CounterState.seconds.value = seconds
                    manager.notify(
                        1,
                        notification(this@CounterService, "Прошло $seconds секунд", ongoing = true),
                    )
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}

class TimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = intent?.getLongExtra("seconds", 0) ?: 0
        if (!isValidTimerDuration(seconds)) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        timerJob?.cancel()
        timerJob = scope.launch {
            delay(seconds * 1_000)
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.areNotificationsEnabled()) {
                manager.notify(
                    2,
                    notification(this@TimerService, "Таймер завершён!", body = "Время вышло"),
                )
            }
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

class RandomService : Service() {
    inner class LocalBinder : Binder() {
        fun service() = this@RandomService
    }

    val number = MutableStateFlow<Int?>(null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            while (isActive) {
                number.value = Random.nextInt(0, 101)
                delay(1_000)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = LocalBinder()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usePracticeSystemBars()
        setContent {
            PracticeShell("Модуль 4", onBack = ::finish) {
                Surface(Modifier.fillMaxSize()) {
                    ServiceDemo(intent.getIntExtra("edu.moduleui.INITIAL_TAB", 0))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceDemo(initialTab: Int) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }

    Box(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxSize().padding(horizontal = 16.dp)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                listOf("Счётчик", "Таймер", "Число").forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            title,
                            modifier = Modifier.padding(horizontal = 4.dp),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (tab) {
                    0 -> CounterScreen()
                    1 -> TimerScreen()
                    else -> RandomNumberScreen()
                }
            }
        }
    }
}

@Composable
private fun CounterScreen() {
    val context = LocalContext.current
    val seconds by CounterState.seconds.collectAsState()
    val start = notificationAction {
        ContextCompat.startForegroundService(context, Intent(context, CounterService::class.java))
    }
    ServiceReading("Прошло секунд", "$seconds")

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = start,
        ) {
            Text("Старт")
        }
        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = { context.stopService(Intent(context, CounterService::class.java)) },
        ) {
            Text("Стоп")
        }
    }
}

private fun isValidTimerDuration(seconds: Long): Boolean = seconds in 1..Long.MAX_VALUE / 1_000

@Composable
private fun TimerScreen() {
    val context = LocalContext.current
    var input by remember { mutableStateOf("30") }
    var status by remember { mutableStateOf("") }
    val start = notificationAction {
        val seconds = input.toLongOrNull()
        if (seconds != null && isValidTimerDuration(seconds)) {
            val intent = Intent(context, TimerService::class.java).putExtra("seconds", seconds)
            context.startService(intent)
            val manager = context.getSystemService(NotificationManager::class.java)
            status = if (manager.areNotificationsEnabled()) {
                "Таймер запущен"
            } else {
                "Таймер запущен, уведомления запрещены"
            }
        }
    }

    OutlinedTextField(
        value = input,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        onValueChange = { input = it },
        label = { Text("Секунды") },
    )
    Button(modifier = Modifier.fillMaxWidth(), onClick = {
        val seconds = input.toLongOrNull()
        if (seconds == null || !isValidTimerDuration(seconds)) {
            status = "Введите положительное число секунд"
        } else {
            start()
        }
    }) {
        Text("Запустить таймер")
    }
    OutlinedButton(
        onClick = {
            val stopped = context.stopService(Intent(context, TimerService::class.java))
            status = if (stopped) "Таймер остановлен" else "Активного таймера нет"
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Остановить таймер") }
    if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun RandomNumberScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    var number by remember { mutableStateOf<Int?>(null) }
    var bound by remember { mutableStateOf(false) }
    var collector by remember { mutableStateOf<Job?>(null) }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val service = (binder as RandomService.LocalBinder).service()
                collector = scope.launch { service.number.collect { number = it } }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                collector?.cancel()
                number = null
            }
        }
    }

    fun disconnect() {
        collector?.cancel()
        if (bound) context.unbindService(connection)
        bound = false
        number = null
    }

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) disconnect()
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            disconnect()
        }
    }

    ServiceReading(if (bound) "Подключено" else "Нет подключения", number?.toString() ?: "—")
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !bound,
        onClick = {
            bound = context.bindService(
                Intent(context, RandomService::class.java),
                connection,
                Context.BIND_AUTO_CREATE,
            )
        },
    ) {
        Text("Подключиться")
    }
    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { disconnect() }) {
        Text("Отключиться")
    }
}

@Composable
private fun ServiceReading(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                value,
                style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(label, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}
