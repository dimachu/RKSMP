package edu.practice14

import edu.moduleui.PracticeShell
import edu.moduleui.usePracticeSystemBars
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usePracticeSystemBars()
        setContent {
            PracticeShell("Модуль 6", onBack = ::finish) {
                Surface(Modifier.fillMaxSize()) {
                    HeartRateScreen(viewModel())
                }
            }
        }
    }
}

@Composable
private fun HeartRateScreen(model: BleModel) {
    val requestPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.all { it }) {
            model.scan()
        } else {
            model.error = "Разрешение Bluetooth отклонено"
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("Монитор сердечного ритма", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeartRateReading(model.bpm, model.status)
                    ScanActions(
                        onScan = { requestPermissions.launch(scanPermissions()) },
                        onDisconnect = model::disconnect,
                    )
                    if (model.error.isNotBlank()) BluetoothError(model.error)
                }
            }
            item {
                Text(
                    "Устройства",
                    Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (model.devices.isEmpty()) {
                item {
                    Text(
                        "Устройства пока не обнаружены",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            items(model.devices.keys.toList(), key = { it }) { address ->
                val nearby = model.devices.getValue(address)
                Surface(
                    onClick = {
                        try {
                            model.connect(nearby.device)
                        } catch (failure: Exception) {
                            model.error = failure.message ?: "Ошибка подключения"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
                            .heightIn(min = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            nearby.advertisedName ?: deviceName(nearby.device),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanActions(onScan: () -> Unit, onDisconnect: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 320.dp && fontScale <= 1.15f) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onScan, modifier = Modifier.weight(1f)) { Text("Сканировать") }
                TextButton(onClick = onDisconnect) { Text("Отключиться") }
            }
        } else {
            Column {
                Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) { Text("Сканировать") }
                TextButton(onClick = onDisconnect) { Text("Отключиться") }
            }
        }
    }
}

@Composable
private fun HeartRateReading(bpm: Int?, status: String) {
    val reading = bpm?.toString() ?: "—"
    val readingStyle = if (reading.length > 3) {
        MaterialTheme.typography.displayMedium
    } else {
        MaterialTheme.typography.displayLarge
    }
    val connection = when (status) {
        "Connected" -> "Подключено"
        "Connecting" -> "Подключение…"
        "Disconnected" -> "Нет подключения"
        else -> status
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Heart Rate", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(reading, Modifier.alignByBaseline(), style = readingStyle)
                Text("bpm", Modifier.alignByBaseline(), style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider()
            Text(connection, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private val connectionError = Regex("^(Ошибка подключения|Соединение разорвано): (\\d+)$")

@Composable
private fun BluetoothError(message: String) {
    val diagnostic = connectionError.matchEntire(message)?.groupValues
    val title = when (diagnostic?.get(1)) {
        "Ошибка подключения" -> "Не удалось подключиться"
        "Соединение разорвано" -> "Соединение разорвано"
        else -> message
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            if (diagnostic != null) {
                Text("Код ${diagnostic[2]}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun scanPermissions() = if (Build.VERSION.SDK_INT >= 31) {
    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
} else {
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
}

@SuppressLint("MissingPermission")
private fun deviceName(device: BluetoothDevice) = try {
    device.name ?: "Без имени"
} catch (_: SecurityException) {
    "Без имени"
}
