package com.example.aegizpoduct.logic

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.HandlerThread
import android.os.Looper
import android.os.Binder
import android.os.Bundle
import androidx.compose.runtime.key
import androidx.compose.ui.semantics.Role
import com.example.aegizpoduct.Model.AppRole
import com.example.aegizpoduct.Model.DemoConfig
import com.example.aegizpoduct.Model.AppUser
import com.example.aegizpoduct.Model.SosEvent
import com.example.aegizpoduct.Model.BleConfig
import com.example.aegizpoduct.Model.BleUiState
import com.example.aegizpoduct.Model.BleStage
import com.example.aegizpoduct.Model.DemoAccount
import com.example.aegizpoduct.Model.Esp32Telemetry
import com.example.aegizpoduct.session.AppSession
import kotlin.text.Regex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.logging.Handler
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

fun scanErrorMessage(errorCode: Int): String = when (errorCode) {
    ScanCallback.SCAN_FAILED_ALREADY_STARTED ->
        "Scan ble, reconnect"
    ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED ->
        "Scan BLE gagal, matikan dan nyalakan bluetoth hp"
    ScanCallback.SCAN_FAILED_INTERNAL_ERROR ->
        "Scan BLE gagal scan ulang."
    ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED ->
        "HP tidak mendukung BLE"
    ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES ->
        "Bluetooth HP penuh"
    else -> "Scan gagal (kode $errorCode)"
}

fun parseBleLine(line : String) : Esp32Telemetry{
    parsePipeSos(line)?.let{return it}
    fun grab(key: String): String? =
        Regex("""\b$key=([^\s|]+)""").find(line)?.groupValues?.getOrNull(1)
    val lat = grab("lat")?.toDoubleOrNull()
    val lng = grab("lng")?.toDoubleOrNull()
    val chars = grab("chars")?.toLongOrNull()
    val gpsFIX = line.contains("GPS:FIX") && lat != null && lng != null
    val lora = when{
        line.contains("LoRa:OK") -> true
        line.contains("LoRa:FAIL") -> false
        else -> null
    }
    val wifi = when{
        line.contains("WiFI:OK") -> true
        line.contains("WIFI:OFF") -> false
        else -> null
    }
    val sosActive = line.contains("SOS:ACTIVE")
    val sosSender =  Regex("""\bsender=([^\s|]+)""").find(line)?.groupValues.getOrNull(1)
    val sosDeviceID = Regex("""\bdevice=([^\s|]+)""").find(line)?.groupValues.getOrNull(1)
    val sosLat = grab("sosLat")?.toDoubleOrNull() ?: if (sosActive) lat else null
    val sosLng = grab("sosLng")?.toDoubleOrNull() ?: grab("sosLon")?.toDoubleOrNull() ?: if (sosActive) lng else null
    val sosSource = Regex("""\bsource=([^\s|]+)""").find(line)?.groupValues?.getOrNull(1)
    val sosPacketTimestamp = grab("sosTs")?.toLongOrNull()
        ?: grab("sosPacketTs")?. toLongOrNull()
        ?: grab("sos_packet_ts")?.toLongOrNull()
    return Esp32Telemetry (
        deviceId = sosDeviceID,
        gpsValid = gpsFIX,
        lat = lat,
        lng = lng,
        charsProcessed = chars,
        loraOk = lora,
        wifiOk = wifi,
        sosActive = sosActive,
        sosSender = sosSender,
        sosLat = sosLat,
        sosLon = sosLng,
        sosSource = sosSource,
        sosPacketTimestamp = sosPacketTimestamp,
        measuredAtEPoch = System.currentTimeMillis() / 1000
    )
}
private  fun parsePipeSos(line: String): Esp32Telemetry?{
    val parts = line.trim().split('|')
    if (parts.size < 6 || !parts[0].equals("SOS", ignoreCase = true)) return null
    val lat = parts[3].toDoubleOrNull()
    val lon = parts[4].toDoubleOrNull()
    val packetTimestamp = parts.getOrNull(6)?.toLongOrNull()
    return Esp32Telemetry(
        deviceId = parts[2],
        gpsValid = lat != null && lon != null,
        lat = lat,
        lng = lon,
        loraOk = true,
        wifiOk = false,
        sosActive = true,
        sosSender = parts[1],
        sosDeviceId = parts[2],
        sosLat = lat,
        sosLon = lon,
        sosSource = parts[5],
        sosPacketTimestamp = packetTimestamp,
        measuredAtEPoch = System.currentTimeMillis() / 1000
    )
}

fun bleNamesFor(Role : AppRole): List<String> = when(Role){
    AppRole.RESCUER -> BleConfig.RESCUER_DEVICE_NAMES
    AppRole.PENANGGUNG_JAWAB -> BleConfig.RECEIVER_DEVICE_NAMES
}
class BleManager( private val appContext: Context) {
    private val _state = MutableStateFlow(BleUiState())
    val state: StateFlow<BleUiState> = _state.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private var gatt: BluetoothGatt? = null
    private var rxChar: BluetoothGattCharacteristic? = null
    private var scanning = false
    private var scanAlreadyStartedRetried = false
    private var scanTargetNames: List<String> = BleConfig.DEVICE_NAMES
    private var connectedDeviceName: String? = null

    private var sosSignature: String? = null
    private var sosFirstSeenMs: Long? = null
    private var dismissedSosSignature: String? = null
    private var locationListener: LocationListener? = null

    @SuppressLint("MissingPermission")
    fun startScan(targetNames: List<String> = BleConfig.DEVICE_NAMES) {
        startScanInternal(targetNames, resetAlreadyStartedRetry = true)
    }

    @SuppressLint("MissingPermission")
    private fun startScanInternal(
        targetNames: List<String> = BleConfig.DEVICE_NAMES,
        resetAlreadyStartedRetry: Boolean,
    ) {
        if (resetAlreadyStartedRetry) scanAlreadyStartedRetried = false
        scanTargetNames = targetNames.ifEmpty { BleConfig.DEVICE_NAMES }
        val a = adapter
        if (a == null) {
            _state.update {
                it.copy(
                    stage = BleStage.ERROR,
                    message = "Perangkat tidak punya Bluetooth"
                )
            }
            return
        }
        if (!a.isEnabled) {
            _state.update { it.copy(stage = BleStage.BLUETOOTH_OFF, message = "Bluetooth mati") }
            return
        }
        if (!hasScanPermission()) {
            _state.update {
                it.copy(
                    stage = BleStage.NO_PERMISSION,
                    message = "Izin Bluetooth belum diberikan"
                )
            }
            return
        }

        startLocationTracking()
        if (scanning) stopScan()
        disconnectInternal()
        val scanner = a.bluetoothLeScanner ?: run {
            _state.update {
                it.copy(
                    stage = BleStage.ERROR,
                    message = "Scanner BLE tidak tersedia"
                )
            }
            return
        }
        runCatching { scanner.stopScan(scanCallback) }

        val filters = emptyList<ScanFilter>()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanning = true
        _state.update {
            it.copy(
                stage = BleStage.SCANNING,
                message = "Mencari ESP32…",
                telemetry = null
            )
        }
        runCatching {
            scanner.startScan(filters, settings, scanCallback)
        }.onFailure { e ->
            scanning = false
            _state.update {
                it.copy(
                    stage = BleStage.DISCONNECTED,
                    message = "Gagal memulai scan BLE: ${e.message}"
                )
            }
            return
        }

        mainHandler.postDelayed({
            if (scanning) {
                stopScan()
                if (_state.value.stage == BleStage.SCANNING) {
                    _state.update {
                        it.copy(
                            stage = BleStage.DISCONNECTED,
                            message = "Perangkat Rescuer01 tidak ditemukan (Timeout)"
                        )
                    }
                }
            }
        }, BleConfig.SCAN_TIMEOUT_MS)
    }
}

fun stableQueueEventId(){

}

