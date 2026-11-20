package edu.practice3

import edu.moduleui.PracticeShell
import edu.moduleui.usePracticeSystemBars
import edu.moduleui.PracticeHeading
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import edu.module4.notificationAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val PHOTO_WORK = "photo"
private const val WEATHER_WORK = "weather"
private const val WEATHER_PROGRESS_ID = 98
private const val WEATHER_REPORT_ID = 99
private val weatherCities = listOf("Москва", "Лондон", "Нью-Йорк")
private val simulatedTemperatures = listOf(-5, 10, 7)
private val weatherNotificationLock = Mutex()

fun notice(context: Context, title: String, ongoing: Boolean): Notification {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
        NotificationChannel("work", "Работа", NotificationManager.IMPORTANCE_LOW),
    )
    return Notification.Builder(context, "work")
        .setSmallIcon(android.R.drawable.ic_menu_upload)
        .setContentTitle(title)
        .setOngoing(ongoing)
        .build()
}

private fun showWeatherProgress(context: Context, completedCities: Int) {
    val manager = context.getSystemService(NotificationManager::class.java)
    if (manager.areNotificationsEnabled()) {
        val total = weatherCities.size
        val title = if (completedCities == total) {
            "Все данные получены, формируем отчёт…"
        } else {
            "Прогноз: $completedCities из $total городов"
        }
        val notification = notice(context, title, true)
        manager.notify(
            WEATHER_PROGRESS_ID,
            Notification.Builder.recoverBuilder(context, notification)
                .setProgress(total, completedCities, false)
                .build(),
        )
    }
}

private suspend fun updateWeatherProgress(context: Context) {
    weatherNotificationLock.withLock {
        val work = withContext(Dispatchers.IO) {
            WorkManager.getInstance(context).getWorkInfosForUniqueWork(WEATHER_WORK).get()
        }
        // WorkManager очищает progress при завершении Worker. Тогда учитываем его состояние.
        val completedCities = work.count { info ->
            WeatherWorker::class.java.name in info.tags &&
                (info.state == WorkInfo.State.SUCCEEDED || info.progress.getBoolean("completed", false))
        }
        showWeatherProgress(context, completedCities.coerceAtMost(weatherCities.size))
    }
}

abstract class PhotoWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    suspend fun progress(label: String) {
        for (step in 0..10) {
            setProgress(workDataOf("label" to label, "progress" to step * 10))
            delay(60)
        }
    }

    suspend fun process(action: suspend () -> String): Result = try {
        val path = action()
        Result.success(workDataOf("path" to path))
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(workDataOf("error" to (exception.message ?: "Ошибка файла")))
    }
}

class CompressWorker(context: Context, params: WorkerParameters) : PhotoWorker(context, params) {
    override suspend fun doWork() = process {
        withContext(Dispatchers.IO) {
            progress("Сжатие")
            val uri = Uri.parse(checkNotNull(inputData.getString("uri")))
            val bitmap = decodePhoto(uri)
            val output = File(applicationContext.filesDir, "compressed_$id.jpg")

            try {
                val ratio = minOf(1f, 1600f / maxOf(bitmap.width, bitmap.height))
                val resized = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt().coerceAtLeast(1),
                    (bitmap.height * ratio).toInt().coerceAtLeast(1),
                    true,
                )
                try {
                    output.outputStream().use {
                        check(resized.compress(Bitmap.CompressFormat.JPEG, 75, it))
                    }
                } finally {
                    if (resized !== bitmap) resized.recycle()
                }
            } finally {
                bitmap.recycle()
            }
            output.absolutePath
        }
    }

    private fun decodePhoto(uri: Uri): Bitmap {
        val resolver = applicationContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Изображение не прочитано" }

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 3200) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Изображение не прочитано")
    }
}

class WatermarkWorker(context: Context, params: WorkerParameters) : PhotoWorker(context, params) {
    override suspend fun doWork() = process {
        withContext(Dispatchers.IO) {
            progress("Водяной знак")
            val source = BitmapFactory.decodeFile(inputData.getString("path")) ?: error("Нет сжатого фото")
            val bitmap = try {
                checkNotNull(source.copy(Bitmap.Config.ARGB_8888, true))
            } finally {
                source.recycle()
            }
            val output = File(applicationContext.filesDir, "watermark_$id.jpg")

            try {
                drawWatermark(bitmap)
                output.outputStream().use {
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it))
                }
            } finally {
                bitmap.recycle()
            }
            output.absolutePath
        }
    }

    private fun drawWatermark(bitmap: Bitmap) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = minOf(bitmap.width / 6f, bitmap.height / 4f, 40f)
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        val inset = (bitmap.width * 0.04f).coerceAtLeast(1f)
        Canvas(bitmap).drawText("Practice 3", inset, bitmap.height * 0.9f, paint)
    }
}

class UploadWorker(context: Context, params: WorkerParameters) : PhotoWorker(context, params) {
    override suspend fun doWork() = process {
        progress("Имитация загрузки")
        val path = checkNotNull(inputData.getString("path"))
        check(File(path).length() > 0)
        path
    }
}

class WeatherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val index = inputData.getInt("index", 0)
        val city = weatherCities[index]
        setForeground(
            ForegroundInfo(
                30 + index,
                notice(applicationContext, "Прогноз: $city — загрузка", true),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            ),
        )
        setProgress(workDataOf("label" to "$city: загрузка"))
        delay(2000L + index * 800)

        val temperature = simulatedTemperatures[index]
        setProgress(workDataOf("label" to "$city: $temperature°C", "completed" to true))
        updateWeatherProgress(applicationContext)
        return Result.success(workDataOf("city$index" to city, "temp$index" to temperature))
    }
}

class ReportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val temperatures = weatherCities.indices.map { inputData.getInt("temp$it", Int.MIN_VALUE) }
        if (Int.MIN_VALUE in temperatures) return Result.failure()

        val forecasts = temperatures.mapIndexed { index, temperature ->
            "${inputData.getString("city$index")}: $temperature°C"
        }
        val report = (forecasts + "Средняя: ${temperatures.average()}°C").joinToString("\n")
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.cancel(WEATHER_PROGRESS_ID)
        if (manager.areNotificationsEnabled()) {
            manager.notify(WEATHER_REPORT_ID, notice(applicationContext, report, false))
        }
        return Result.success(workDataOf("report" to report))
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usePracticeSystemBars()
        setContent {
            PracticeShell("Модуль 4", onBack = ::finish) {
                Surface(Modifier.fillMaxSize()) {
                    WorkDemo(intent.getIntExtra("edu.moduleui.INITIAL_TAB", 0))
                }
            }
        }
    }
}

@Composable
private fun WorkDemo(initialTab: Int) {
    val context = LocalContext.current
    val manager = remember(context) { WorkManager.getInstance(context) }
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selected ->
        if (selected != null) {
            photoUri = try {
                context.contentResolver.takePersistableUriPermission(
                    selected,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                selected.toString()
            } catch (exception: SecurityException) {
                null
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PracticeHeading("Фоновые задачи")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        modifier = Modifier.weight(1f),
                        label = { Text("Фото") },
                    )
                    FilterChip(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        modifier = Modifier.weight(1f),
                        label = { Text("Погода") },
                    )
                }
            }

            if (tab == 0) {
                PhotoScreen(manager, photoUri, onChoosePhoto = { picker.launch(arrayOf("image/*")) })
            } else {
                WeatherScreen(manager)
            }
        }
    }
}

@Composable
private fun PhotoScreen(manager: WorkManager, photoUri: String?, onChoosePhoto: () -> Unit) {
    val workFlow = remember(manager) { manager.getWorkInfosForUniqueWorkFlow(PHOTO_WORK) }
    val work by workFlow.collectAsState(initial = emptyList())
    val running = work.any { !it.state.isFinished }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (photoUri != null) "Фото выбрано" else "Фото не выбрано",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(
                enabled = !running,
                onClick = onChoosePhoto,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Выбрать фото") }
            Button(
                enabled = !running && photoUri != null,
                onClick = { photoUri?.let { enqueuePhotoWork(manager, it) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Начать обработку и загрузку", textAlign = TextAlign.Center) }
        }
    }

    val current = work.firstOrNull { it.state == WorkInfo.State.RUNNING }
    val error = work.firstNotNullOfOrNull { it.outputData.getString("error") }
    val finished = work.size == 3 && work.all { it.state == WorkInfo.State.SUCCEEDED }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            current != null -> {
                val percent = current.progress.getInt("progress", 0)
                Text(
                    current.progress.getString("label") ?: "Подготовка…",
                    style = MaterialTheme.typography.titleLarge,
                )
                LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
                Text("$percent%", style = MaterialTheme.typography.labelLarge)
            }
            running -> {
                Text("Обработка запланирована…", style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            error != null -> Text("Ошибка обработки: $error", color = MaterialTheme.colorScheme.error)
            finished -> {
                Text("Готово! Фото загружено (имитация)", style = MaterialTheme.typography.titleLarge)
                val upload = work.firstOrNull { UploadWorker::class.java.name in it.tags }
                upload?.outputData?.getString("path")?.let { path ->
                    Text(path, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun enqueuePhotoWork(manager: WorkManager, photoUri: String) {
    val compress = OneTimeWorkRequestBuilder<CompressWorker>()
        .setInputData(workDataOf("uri" to photoUri))
        .build()
    val watermark = OneTimeWorkRequestBuilder<WatermarkWorker>().build()
    val upload = OneTimeWorkRequestBuilder<UploadWorker>().build()

    manager.beginUniqueWork(PHOTO_WORK, ExistingWorkPolicy.KEEP, compress)
        .then(watermark)
        .then(upload)
        .enqueue()
}

@Composable
private fun WeatherScreen(manager: WorkManager) {
    val context = LocalContext.current
    val workFlow = remember(manager) { manager.getWorkInfosForUniqueWorkFlow(WEATHER_WORK) }
    val work by workFlow.collectAsState(initial = emptyList())
    val running = work.any { !it.state.isFinished }
    val start = notificationAction {
        showWeatherProgress(context, 0)
        enqueueWeatherWork(manager)
    }

    Button(
        enabled = !running,
        modifier = Modifier.fillMaxWidth(),
        onClick = start,
    ) {
        Text("Собрать прогноз")
    }

    val report = work.firstNotNullOfOrNull { it.outputData.getString("report") }?.lines()
    val progressByCity = work.mapNotNull { it.progress.getString("label") }
        .associate { it.substringBefore(':') to it.substringAfter(':').trim() }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            weatherCities.forEachIndexed { index, city ->
                if (index > 0) HorizontalDivider()
                val cityWork = work.firstOrNull { it.outputData.getString("city$index") == city }
                val completed = cityWork?.outputData?.getInt("temp$index", 0)
                val value = report?.getOrNull(index)?.substringAfter(':')?.trim()
                    ?: completed?.let { "$it°C" } ?: progressByCity[city] ?: "Ожидание"
                WeatherCityRow(city, value)
            }
        }
    }
    if (report != null) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                report.last(),
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    } else if (work.any { it.state == WorkInfo.State.FAILED }) {
        Text("Не удалось собрать прогноз", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun WeatherCityRow(city: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(city, Modifier.weight(1.2f), style = MaterialTheme.typography.titleSmall)
        Text(
            value,
            Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}

private fun enqueueWeatherWork(manager: WorkManager) {
    val forecasts = weatherCities.indices.map { index ->
        OneTimeWorkRequestBuilder<WeatherWorker>()
            .setInputData(workDataOf("index" to index))
            .build()
    }
    manager.beginUniqueWork(WEATHER_WORK, ExistingWorkPolicy.KEEP, forecasts)
        .then(OneTimeWorkRequestBuilder<ReportWorker>().build())
        .enqueue()
}
