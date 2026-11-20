package edu.practice14

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val heartRateService = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
private val heartRateMeasurement = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
private val clientConfiguration = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

data class NearbyDevice(val device: BluetoothDevice, val advertisedName: String?)

@SuppressLint("MissingPermission")
class BleModel(application: Application) : AndroidViewModel(application) {
    private val manager = application.getSystemService(BluetoothManager::class.java)
    private val adapter get() = manager?.adapter
    private var gatt: BluetoothGatt? = null
    private var scanning = false
    private var scanTimeout: Job? = null
    private var connectionTimeout: Job? = null

    val devices = mutableStateMapOf<String, NearbyDevice>()
    var status by mutableStateOf("Disconnected")
    var bpm by mutableStateOf<Int?>(null)
    var error by mutableStateOf("")

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            onMainThread {
                if (!scanning) return@onMainThread
                val address = result.device.address
                val name = result.scanRecord?.deviceName ?: devices[address]?.advertisedName
                devices[address] = NearbyDevice(result.device, name)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            onMainThread {
                scanning = false
                error = "Ошибка сканирования: $errorCode"
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(connection: BluetoothGatt, status: Int, newState: Int) {
            onMainThread {
                if (connection !== gatt) return@onMainThread
                if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                    val message = when {
                        status == BluetoothGatt.GATT_SUCCESS -> "Соединение разорвано"
                        this@BleModel.status == "Connected" -> "Соединение разорвано: $status"
                        else -> "Ошибка подключения: $status"
                    }
                    failConnection(message)
                } else if (newState == BluetoothProfile.STATE_CONNECTED && !connection.discoverServices()) {
                    failConnection("Не удалось начать обнаружение сервисов")
                }
            }
        }

        override fun onServicesDiscovered(connection: BluetoothGatt, status: Int) {
            onMainThread {
                if (connection === gatt) subscribeToHeartRate(connection, status)
            }
        }

        override fun onDescriptorWrite(connection: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            onMainThread {
                if (connection !== gatt) return@onMainThread
                if (status == BluetoothGatt.GATT_SUCCESS && descriptor.uuid == clientConfiguration) {
                    connectionTimeout?.cancel()
                    this@BleModel.status = "Connected"
                } else {
                    failConnection("CCCD отклонён: $status")
                }
            }
        }

        override fun onCharacteristicChanged(
            connection: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            receiveMeasurement(connection, characteristic, value)
        }

        @Deprecated("Legacy callback")
        override fun onCharacteristicChanged(
            connection: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            @Suppress("DEPRECATION")
            val value = characteristic.value ?: return
            receiveMeasurement(connection, characteristic, value)
        }
    }

    fun scan() {
        error = ""
        stopScan()
        devices.clear()
        if (adapter == null) {
            error = "BLE не поддерживается"
            return
        }
        if (adapter?.isEnabled != true) {
            error = "Включите Bluetooth"
            return
        }

        try {
            scanning = true
            adapter?.bluetoothLeScanner?.startScan(scanCallback)
            scanTimeout = viewModelScope.launch {
                delay(15_000)
                stopScan()
            }
        } catch (failure: Exception) {
            scanning = false
            error = failure.message ?: "Ошибка scan"
        }
    }

    fun stopScan() {
        scanTimeout?.cancel()
        if (scanning) {
            try {
                adapter?.bluetoothLeScanner?.stopScan(scanCallback)
            } catch (_: SecurityException) {
                // The scan permission can be revoked while scanning.
            }
        }
        scanning = false
    }

    fun connect(device: BluetoothDevice) {
        disconnect()
        status = "Connecting"
        error = ""
        gatt = device.connectGatt(getApplication(), false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        if (gatt == null) {
            failConnection("Не удалось открыть GATT")
            return
        }

        connectionTimeout = viewModelScope.launch {
            delay(15_000)
            if (status == "Connecting") failConnection("Истекло время подключения")
        }
    }

    fun disconnect() {
        stopScan()
        connectionTimeout?.cancel()
        val connection = gatt
        gatt = null
        try {
            connection?.disconnect()
        } catch (_: SecurityException) {
            // Still attempt to release the GATT client.
        }
        try {
            connection?.close()
        } catch (_: SecurityException) {
            // Bluetooth permission can be revoked during a connection.
        }
        status = "Disconnected"
        bpm = null
    }

    private fun subscribeToHeartRate(connection: BluetoothGatt, status: Int) {
        val characteristic = connection.getService(heartRateService)?.getCharacteristic(heartRateMeasurement)
        val descriptor = characteristic?.getDescriptor(clientConfiguration)
        if (
            status != BluetoothGatt.GATT_SUCCESS || characteristic == null || descriptor == null ||
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0
        ) {
            failConnection("Устройство не поддерживает Heart Rate Notify/CCCD")
            return
        }
        if (!connection.setCharacteristicNotification(characteristic, true)) {
            failConnection("Не удалось включить notifications")
            return
        }
        if (!enableNotifications(connection, descriptor)) {
            failConnection("Ошибка записи CCCD")
        }
    }

    @Suppress("DEPRECATION")
    private fun enableNotifications(connection: BluetoothGatt, descriptor: BluetoothGattDescriptor): Boolean {
        val enable = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        if (Build.VERSION.SDK_INT >= 33) {
            return connection.writeDescriptor(descriptor, enable) == BluetoothStatusCodes.SUCCESS
        }

        descriptor.value = enable
        return connection.writeDescriptor(descriptor)
    }

    private fun receiveMeasurement(
        connection: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        bytes: ByteArray,
    ) {
        onMainThread {
            if (connection !== gatt || characteristic.uuid != heartRateMeasurement) return@onMainThread
            val value = decodeHeartRate(bytes)
            if (value == null) {
                error = "Короткий пакет Heart Rate"
            } else {
                bpm = value
                error = ""
            }
        }
    }

    private fun failConnection(message: String) {
        error = message
        disconnect()
    }

    private fun onMainThread(action: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main.immediate) { action() }
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }
}
