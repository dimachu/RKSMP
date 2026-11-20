package edu.practice4

import edu.moduleui.PracticeShell
import edu.moduleui.usePracticeSystemBars
import edu.moduleui.PracticeHeading
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class LocationModel : ViewModel() {
    var result by mutableStateOf("Нажмите кнопку, чтобы определить адрес")
    var coordinates by mutableStateOf("")
    var busy by mutableStateOf(false)

    fun locate(context: Context) {
        if (busy) return
        busy = true
        result = ""
        coordinates = ""

        viewModelScope.launch {
            try {
                val location = currentLocation(context)
                coordinates = formatCoordinates(location)
                result = withTimeout(15_000) { findAddress(context, location) }
            } catch (exception: TimeoutCancellationException) {
                result = "Истекло время ожидания"
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                result = exception.message ?: "Не удалось получить адрес"
            } finally {
                busy = false
            }
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun currentLocation(context: Context): Location {
    val manager = context.getSystemService(LocationManager::class.java)
    check(manager.isLocationEnabled) { "Геолокация выключена" }
    val playServices = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
    check(playServices == ConnectionResult.SUCCESS) { "Google Play services недоступны" }

    val precise = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val priority = if (precise) {
        Priority.PRIORITY_HIGH_ACCURACY
    } else {
        Priority.PRIORITY_BALANCED_POWER_ACCURACY
    }

    val client = LocationServices.getFusedLocationProviderClient(context)
    val cancellation = CancellationTokenSource()
    val location = try {
        withTimeout(15_000) {
            client.getCurrentLocation(priority, cancellation.token).await()
        }
    } finally {
        cancellation.cancel()
    }
    return checkNotNull(location) { "Координаты не получены" }
}

private fun formatCoordinates(location: Location): String {
    val latitude = "%.5f".format(Locale.ROOT, location.latitude)
    val longitude = "%.5f".format(Locale.ROOT, location.longitude)
    return "Lat: $latitude · Lng: $longitude"
}

private suspend fun findAddress(context: Context, location: Location): String {
    check(Geocoder.isPresent()) { "Geocoder отсутствует" }
    val geocoder = Geocoder(context, Locale.getDefault())
    return if (Build.VERSION.SDK_INT >= 33) {
        suspendCancellableCoroutine { continuation ->
            val listener = object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    if (continuation.isActive) continuation.resume(firstAddressLine(addresses))
                }

                override fun onError(errorMessage: String?) {
                    if (continuation.isActive) {
                        continuation.resume("Ошибка геокодирования: $errorMessage")
                    }
                }
            }
            geocoder.getFromLocation(location.latitude, location.longitude, 1, listener)
        }
    } else {
        withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            firstAddressLine(addresses)
        }
    }
}

private fun firstAddressLine(addresses: List<Address>?): String =
    addresses?.firstOrNull()?.getAddressLine(0) ?: "Адрес не найден"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usePracticeSystemBars()
        setContent {
            PracticeShell("Модуль 4", onBack = ::finish) {
                Surface(Modifier.fillMaxSize()) {
                    LocationScreen()
                }
            }
        }
    }
}

@Composable
private fun LocationScreen(model: LocationModel = viewModel()) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) {
            model.locate(context.applicationContext)
        } else {
            model.coordinates = ""
            model.result = "Разрешение на местоположение отклонено"
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxSize().padding(horizontal = 18.dp),
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PracticeHeading("Местоположение")
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(model.result, style = MaterialTheme.typography.titleLarge)
                        if (model.coordinates.isNotEmpty()) {
                            HorizontalDivider()
                            CoordinateRows(model.coordinates)
                        }
                    }
                }
            }
            HorizontalDivider()
            Button(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                enabled = !model.busy,
                onClick = {
                    permission.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        ),
                    )
                },
            ) {
                Text("Получить мой адрес")
            }
        }
    }
}

@Composable
private fun CoordinateRows(coordinates: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        coordinates.split(" · ").forEach { coordinate ->
            val label = if (coordinate.startsWith("Lat:")) "Широта (Lat)" else "Долгота (Lng)"
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(coordinate.substringAfter(": "), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
