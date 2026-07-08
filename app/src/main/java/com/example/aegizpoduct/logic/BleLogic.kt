package com.example.aegizpoduct.logic

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.aegizpoduct.model.AppRole
import com.example.aegizpoduct.model.BleConfig
import com.example.aegizpoduct.model.BleStage
import com.example.aegizpoduct.model.BleUiState
import com.example.aegizpoduct.model.DemoConfig
import com.example.aegizpoduct.model.Esp32Telemetry
import com.example.aegizpoduct.model.SosEvent
import com.example.aegizpoduct.session.AppSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject


fun scanErrorMessage(errorCode: Int): String = when (errorCode) {
    ScanCallback.SCAN_FAILED_ALREADY_STARTED ->
        "Scan BLE sudah berjalan. Coba tekan Putuskan, lalu scan ulang."
    ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED ->
        "Scan BLE gagal registrasi. Matikan/nyalakan Bluetooth lalu scan ulang."
    ScanCallback.SCAN_FAILED_INTERNAL_ERROR ->
        "Scan BLE gagal karena error internal Android. Matikan/nyalakan Bluetooth lalu scan ulang."
    ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED ->
        "HP tidak mendukung mode scan BLE yang dibutuhkan."
    ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES ->
        "Resource Bluetooth HP penuh. Tutup app BLE lain lalu scan ulang."
    else -> "Scan gagal (kode $errorCode)"
}

fun parseBleLine(line: String): Esp32Telemetry {
    parsePipeSos(line)?.let { return it }

    fun grab(key: String): String? =
        Regex("""\b$key=([^\s|]+)""").find(line)?.groupValues?.getOrNull(1)
    val lat = grab("lat")?.toDoubleOrNull()
    val lng = grab("lng")?.toDoubleOrNull()
    val chars = grab("chars")?.toLongOrNull()
    val gpsFix = line.contains("GPS:FIX") && lat != null && lng != null
    val lora = when {
        line.contains("LoRa:OK") -> true
        line.contains("LoRa:FAIL") -> false
        else -> null
    }
    val wifi = when {
        line.contains("WiFi:OK") -> true
        line.contains("WiFi:OFF") -> false
        else -> null
    }
    val sosActive = line.contains("SOS:ACTIVE")
    val sosSender = Regex("""\bsender=([^\s|]+)""").find(line)?.groupValues?.getOrNull(1)
    val sosDeviceId = Regex("""\bdevice=([^\s|]+)""").find(line)?.groupValues?.getOrNull(1)
    val sosLat = grab("sosLat")?.toDoubleOrNull() ?: if (sosActive) lat else null
    val sosLon = grab("sosLng")?.toDoubleOrNull() ?: grab("sosLon")?.toDoubleOrNull() ?: if (sosActive) lng else null
    val sosSource = Regex("""\bsource=([^\s|]+)""").find(line)?.groupValues?.getOrNull(1)
    val sosPacketTimestamp = grab("sosTs")?.toLongOrNull()
        ?: grab("sosPacketTs")?. toLongOrNull()
        ?: grab("sos_packet_ts")?.toLongOrNull()
    return Esp32Telemetry(
        deviceId = sosDeviceId,
        gpsValid = gpsFix,
        lat = lat,
        lng = lng,
        charsProcessed = chars,
        loraOk = lora,
        wifiOk = wifi,
        sosActive = sosActive,
        sosSender = sosSender,
        sosDeviceId = sosDeviceId,
        sosLat = sosLat,
        sosLon = sosLon,
        sosSource = sosSource,
        sosPacketTimestamp = sosPacketTimestamp,
        measuredAtEpoch = System.currentTimeMillis() / 1000,
    )
}

private fun parsePipeSos(line: String): Esp32Telemetry? {
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
        measuredAtEpoch = packetTimestamp ?: System.currentTimeMillis() / 1000,
    )
}


fun bleNamesFor(role: AppRole): List<String> = when (role) {
    AppRole.RESCUER -> BleConfig.RESCUER_DEVICE_NAMES
    AppRole.PENANGGUNG_JAWAB -> BleConfig.RECEIVER_DEVICE_NAMES
}

class BleManager(private val appContext: Context) {

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
            _state.update { it.copy(stage = BleStage.ERROR, message = "Perangkat tidak punya Bluetooth") }
            return
        }
        if (!a.isEnabled) {
            _state.update { it.copy(stage = BleStage.BLUETOOTH_OFF, message = "Bluetooth mati") }
            return
        }
        if (!hasScanPermission()) {
            _state.update { it.copy(stage = BleStage.NO_PERMISSION, message = "Izin Bluetooth belum diberikan") }
            return
        }

        startLocationTracking()
        if (scanning) stopScan()
        disconnectInternal()
        val scanner = a.bluetoothLeScanner ?: run {
            _state.update { it.copy(stage = BleStage.ERROR, message = "Scanner BLE tidak tersedia") }
            return
        }
        runCatching { scanner.stopScan(scanCallback) }

        val filters = emptyList<ScanFilter>()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanning = true
        _state.update { it.copy(stage = BleStage.SCANNING, message = "Mencari ESP32…", telemetry = null) }
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

    @SuppressLint("MissingPermission")
    fun disconnect() {
        stopScan()
        // stopLocationTracking() // Keep location tracking active so map adjusts to phone GPS!
        disconnectInternal()
        _state.update { it.copy(stage = BleStage.DISCONNECTED, message = "Terputus", telemetry = null) }
    }

    @SuppressLint("MissingPermission")
    fun sendLine(text: String): Boolean {
        val g = gatt ?: return false
        val ch = rxChar ?: return false
        if (!hasConnectPermission()) return false
        val bytes = text.toByteArray(Charsets.UTF_8)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeCharacteristic(ch, bytes, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) ==
                    BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            run {
                ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                ch.value = bytes
                g.writeCharacteristic(ch)
            }
        }
    }

    // Kirim UID rescuer yang sedang login ke ESP32 lewat BLE, supaya tombol SOS fisik
    // melaporkan identitas yang benar (bukan ID default firmware). WRITE_TYPE_NO_RESPONSE
    // tidak ada ACK, jadi dikirim beberapa kali untuk menaikkan peluang sampai.
    @SuppressLint("MissingPermission")
    private fun pushRescuerIdentity() {
        val payload = "ID|${AppSession.currentRescuerId()}"
        sendLine(payload)
        mainHandler.postDelayed({ sendLine(payload) }, 400)
        mainHandler.postDelayed({ sendLine(payload) }, 1200)
    }

    fun onPermissionDenied() {
        _state.update { it.copy(stage = BleStage.NO_PERMISSION, message = "Izin Bluetooth ditolak") }
    }

    fun bluetoothEnabled(): Boolean = adapter?.isEnabled == true

    fun dismissSos() {
        dismissedSosSignature = sosSignature
        _state.update { st ->
            val t = st.telemetry
            if (t != null && t.sosActive) st.copy(telemetry = t.copy(sosActive = false)) else st
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        if (!scanning) return
        scanning = false
        runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!scanning) return
            val device = result.device
            val name = runCatching { device.name }.getOrNull()
                ?: result.scanRecord?.deviceName
                ?: ""

            val hasNus = result.scanRecord?.serviceUuids?.any {
                it.uuid.toString().equals(BleConfig.NUS_SERVICE_UUID.toString(), ignoreCase = true)
            } == true

            if (!nameMatchesTarget(name) && !hasNus) return

            stopScan()
            val displayName = if (name.isNotBlank()) name else "ESP32 Kalung"
            connectedDeviceName = displayName
            _state.update { it.copy(stage = BleStage.CONNECTING, message = "Menghubungkan ke $displayName…") }
            connect(device)
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
            if (errorCode == ScanCallback.SCAN_FAILED_ALREADY_STARTED && !scanAlreadyStartedRetried) {
                scanAlreadyStartedRetried = true
                runCatching { adapter?.bluetoothLeScanner?.stopScan(this) }
                _state.update { it.copy(stage = BleStage.SCANNING, message = "Scan lama masih aktif, mencoba ulang...") }
                mainHandler.postDelayed({
                    startScanInternal(scanTargetNames, resetAlreadyStartedRetry = false)
                }, 500)
                return
            }
            _state.update { it.copy(stage = BleStage.ERROR, message = scanErrorMessage(errorCode)) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        if (!hasConnectPermission()) {
            _state.update { it.copy(stage = BleStage.NO_PERMISSION, message = "Izin koneksi Bluetooth belum ada") }
            return
        }
        gatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    private fun disconnectInternal() {
        // stopLocationTracking() // Keep location tracking active so map adjusts to phone GPS!
        gatt?.let {
            runCatching { it.disconnect() }
            runCatching { it.close() }
        }
        gatt = null
        rxChar = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (g !== gatt) {
                runCatching { g.close() }
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _state.update { it.copy(stage = BleStage.CONNECTING, message = "Negosiasi MTU…") }
                    if (!g.requestMtu(247)) g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    disconnectInternal()
                    if (_state.value.stage != BleStage.DISCONNECTED) {
                        _state.update { it.copy(stage = BleStage.DISCONNECTED, message = "Koneksi terputus") }
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            g.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _state.update { it.copy(stage = BleStage.ERROR, message = "Gagal discover service (status $status)") }
                return
            }
            val txChar = g.getService(BleConfig.NUS_SERVICE_UUID)?.getCharacteristic(BleConfig.NUS_TX_UUID)
            if (txChar == null) {
                _state.update {
                    it.copy(
                        stage = BleStage.ERROR,
                        message = "NUS tidak ditemukan. Lupakan pairing manual di Setelan Bluetooth, lalu scan ulang.",
                    )
                }
                return
            }

            rxChar = g.getService(BleConfig.NUS_SERVICE_UUID)?.getCharacteristic(BleConfig.NUS_RX_UUID)

            g.setCharacteristicNotification(txChar, true)
            val cccd = txChar.getDescriptor(BleConfig.CCCD_UUID)
            if (cccd != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    g.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    g.writeDescriptor(cccd)
                }
            }
            _state.update { it.copy(stage = BleStage.CONNECTED, message = "Terhubung — menerima data") }
            pushRescuerIdentity()
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic, value: ByteArray) {
            onLine(String(value, Charsets.UTF_8))
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            val v = ch.value ?: return
            onLine(String(v, Charsets.UTF_8))
        }
    }

    private fun applySosTracking(t: Esp32Telemetry): Esp32Telemetry {
        if (!t.sosActive) {
            sosSignature = null
            sosFirstSeenMs = null
            return t
        }
        val sig = listOf(t.sosSender, t.sosDeviceId, t.sosLat, t.sosLon, t.sosSource, t.sosPacketTimestamp).joinToString("|")
        if (sig != sosSignature) {
            sosSignature = sig
            sosFirstSeenMs = System.currentTimeMillis()
        }
        val dismissed = sig == dismissedSosSignature
        return t.copy(sosActive = !dismissed, sosStartMs = sosFirstSeenMs)
    }

    private fun onLine(raw: String) {
        val line = raw.trim()
        if (line.isEmpty()) return
        val parsed = applySosTracking(parseBleLine(line))
        _state.update {
            val tel = parsed.copy(deviceId = parsed.deviceId ?: connectedDeviceName)
            it.copy(stage = BleStage.CONNECTED, telemetry = tel, message = "Terhubung — menerima data")
        }
    }

    private fun hasScanPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) granted(Manifest.permission.BLUETOOTH_SCAN)
        else granted(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun hasConnectPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) granted(Manifest.permission.BLUETOOTH_CONNECT) else true

    private fun granted(perm: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, perm) == PackageManager.PERMISSION_GRANTED

    private fun nameMatchesTarget(name: String): Boolean {
        if (name.isBlank()) return false
        val n = name.trim()
        if (scanTargetNames.any { n.equals(it, ignoreCase = true) || n.contains(it, ignoreCase = true) || it.contains(n, ignoreCase = true) }) return true
        val keywords = listOf("aegiz", "aegis", "rescuer", "penanggung", "sar", "lora", "pj01", "r01")
        return keywords.any { n.contains(it, ignoreCase = true) }
    }

    @SuppressLint("MissingPermission")
    fun startLocationTracking() {
        if (!granted(Manifest.permission.ACCESS_FINE_LOCATION) && !granted(Manifest.permission.ACCESS_COARSE_LOCATION)) return
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        val lastLoc = runCatching {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }.getOrNull()
        if (lastLoc != null) {
            _state.update { it.copy(phoneLat = lastLoc.latitude, phoneLon = lastLoc.longitude) }
        }
        if (locationListener == null) {
            locationListener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    _state.update { it.copy(phoneLat = loc.latitude, phoneLon = loc.longitude) }
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            val listener = locationListener!!
            runCatching {
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 2f, listener, Looper.getMainLooper())
                }
            }
            runCatching {
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 2f, listener, Looper.getMainLooper())
                }
            }
        }
    }

    private fun stopLocationTracking() {
        val listener = locationListener ?: return
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        runCatching { lm.removeUpdates(listener) }
        locationListener = null
    }
}


fun SosEvent.stableQueueEventId(): String =
    eventId?.takeIf { it.isNotBlank() } ?: "hp_${createdAt}_${deviceId}_${source}".sanitizeSosQueueEventId()

private fun String.sanitizeSosQueueEventId(): String =
    replace(Regex("""[^A-Za-z0-9_-]"""), "_")

class SosOfflineQueue(context: Context) {
    private val prefs = context.getSharedPreferences("aegiz_sos_offline_queue", Context.MODE_PRIVATE)

    @Synchronized
    fun all(): List<SosEvent> = readArray().toEvents()

    @Synchronized
    fun count(): Int = readArray().length()

    @Synchronized
    fun enqueue(event: SosEvent) {
        val eventWithId = event.withStableId()
        val existing = all()
        if (existing.any { it.eventId == eventWithId.eventId }) return
        writeEvents(existing + eventWithId)
    }

    @Synchronized
    fun remove(eventId: String?) {
        if (eventId.isNullOrBlank()) return
        writeEvents(all().filterNot { it.eventId == eventId })
    }

    private fun readArray(): JSONArray {
        val raw = prefs.getString(KEY_EVENTS, "[]").orEmpty()
        return runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
    }

    private fun writeEvents(events: List<SosEvent>) {
        val arr = JSONArray()
        events.forEach { arr.put(it.withStableId().toQueueJson()) }
        prefs.edit().putString(KEY_EVENTS, arr.toString()).apply()
    }

    private fun JSONArray.toEvents(): List<SosEvent> =
        (0 until length()).mapNotNull { index -> optJSONObject(index)?.toQueueEvent() }

    private fun SosEvent.withStableId(): SosEvent =
        if (!eventId.isNullOrBlank()) this else copy(eventId = stableQueueEventId())

    private fun SosEvent.toQueueJson(): JSONObject =
        JSONObject()
            .put("event_id", eventId)
            .put("mission_id", missionId)
            .put("rescuer_id", rescuerId)
            .put("rescuer_name", rescuerName)
            .put("device_id", deviceId)
            .put("status", status)
            .put("source", source)
            .put("lat", lat)
            .put("lon", lon)
            .put("created_at", createdAt)
            .put("sos_packet_ts", sosPacketTimestamp)

    private fun JSONObject.toQueueEvent(): SosEvent =
        SosEvent(
            eventId = optStringOrNull("event_id"),
            missionId = optStringOrNull("mission_id") ?: DemoConfig.MISSION_ID,
            rescuerId = optStringOrNull("rescuer_id") ?: DemoConfig.RESCUER_ID,
            rescuerName = optStringOrNull("rescuer_name") ?: DemoConfig.RESCUER_NAME,
            deviceId = optStringOrNull("device_id") ?: DemoConfig.DEVICE_ID,
            status = optStringOrNull("status") ?: "DARURAT",
            source = optStringOrNull("source") ?: "hp_internet",
            lat = optDoubleOrNull("lat"),
            lon = optDoubleOrNull("lon"),
            createdAt = optLongOrNull("created_at") ?: 0L,
            sosPacketTimestamp = optLongOrNull("sos_packet_ts"),
        )

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key) else null

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) optLong(key) else null

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (has(key) && !isNull(key)) optDouble(key) else null

    private companion object {
        const val KEY_EVENTS = "events"
    }
}

