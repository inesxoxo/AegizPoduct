package com.example.aegizpoduct.logic

import android.Manifest
import android.R
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
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Looper
import android.os.Binder
import android.os.Bundle
import androidx.compose.runtime.key
import com.example.aegizpoduct.Model.AppRole
import com.example.aegizpoduct.Model.DemoConfig
import com.example.aegizpoduct.Model.AppUser
import com.example.aegizpoduct.Model.SosEvent
import com.example.aegizpoduct.Model.BleConfig
import com.example.aegizpoduct.Model.BleUiState
import com.example.aegizpoduct.Model.BleStage
import com.example.aegizpoduct.Model.DemoAccount
import com.example.aegizpoduct.Model.Esp32Telemetry
//app session kudune sih
import kotlin.text.Regex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

fun scanErrorMessage(errorCode: Int): String = when (errorCode) {
    ScanCallback.SCAN_FAILED_ALREADY_STARTED ->
        "Scan ble, reconnect"
    ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED ->
        "Scan BLE gagal, matikan dan nyalakan bluetoth hp"
    ScanCallback.SCAN_FAILED_INTERNAL_ERROR ->
        "Scan BLE gagal karena error internal Android. Matikan/nyalakan Bluetooth lalu scan ulang."
    ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED ->
        "HP tidak mendukung BLE"
    ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES ->
        "Bluetooth HP penuh"
    else -> "Scan gagal (kode $errorCode)"
}

fun parseBleLine(line : String) : Esp32Telemetry{
    parsePipeSos(line)?.let{ return it }
    fun grab(key: String): String? =
        Regex("""\b$key=([^\s|]+)""").find(line)?.groupValues?.getOrNull(1)
    val lat = grab("lat")?.toDoubleOrNull()
    val lng = grab("lng")?.toDoubleOrNull()
    val lora = when{
        line.contains("LoRa:OK") -> true
        line.contains("LoRa:FAIL") -> false
        else -> null
    }
    val wifi = when{
        line.contains("WIFI:OK") -> true
        line.contains("WIFI:False") -> false
        else -> null
    }
    val sosActive = line.contains("SOS:Active")
    val sosSender =  Regex("""\b$key=([^\s|]+)""").find(line)?.groupValues.getOrNull(1)
    val sosDeviceID = Regex("""\b$key=([^\s|]+)""").find(line)?.groupValues.getOrNull(1)
}

fun stableQueueEventId(){

}

