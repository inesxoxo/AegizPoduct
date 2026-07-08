package com.example.aegizpoduct.Model

import java.util.UUID

enum class AppRole {RESCUER, PENANGGUNG_JAWAB}

data class AppUser (
    val uid: String = "",
    val fullname: String = "",
    val email: String = "",
    val role: String = "",
    val createdAt: Long = 0L,
)

data class DemoAccount(
    val username: String,
    val password: String,
    val role: AppRole,
    val userId: String,
    val displayName: String,
)

object DemoConfig{
    const val MISSION_ID = "DEMO-01"
    const val MISSION_NAME = "Misi 01"
    const val RESPONSIBLE_ID = "PJ001"
    const val RESPONSIBLE_NAME = "Penanggung Jawab 01"
    const val RESCUER_ID = "R001"
    const val RESCUER_NAME = "Rescuer 01"
    const val DEVICE_ID = "DEV001"

    val accounts = listOf(
        DemoAccount(
            username = "penanggungJawab01",
            password = "demo123",
            role = AppRole.PENANGGUNG_JAWAB,
            userId = RESPONSIBLE_ID,
            displayName = RESPONSIBLE_NAME
        ),
        DemoAccount(
            username = "Rescuer01",
            password = "demo123",
            role = AppRole.RESCUER,
            userId = RESCUER_ID,
            displayName = RESCUER_NAME,
        ),
    )
}

enum class BleStage { IDLE, BLUETOOTH_OFF, NO_PERMISSION, SCANNING, NOT_FOUND, CONNECTING, CONNECTED, DISCONNECTED, ERROR }

object BleConfig {
    val RESCUER_DEVICE_NAMES = listOf("rescuer 01", "Rescuer01")
    val RECEIVER_DEVICE_NAMES = listOf("penanggung jawab 01", "Penanggungjawab01")
    val DEVICE_NAMES = RESCUER_DEVICE_NAMES + RECEIVER_DEVICE_NAMES
    val NUS_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val NUS_TX_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    val NUS_RX_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    const val SCAN_TIMEOUT_MS = 12_000L
}

data class Esp32Telemetry(
    val deviceId: String? = null,
    val measuredAtEPoch: Long? = null,
    val gpsValid: Boolean = false,
    val lat: Double? = null,
    val lng: Double? = null,
    val charsProcessed: Long? = null,
    val loraOk: Boolean? = null,
    val wifiOk: Boolean? = null,
    val sosActive: Boolean = false,
    val sosSender: String? = null,
    val sosDeviceId: String? = null,
    val sosLat: Double? = null,
    val sosLon: Double? = null,
    val sosSource: String? = null,
    val sosPacketTimestamp: Long? = null,
    val sosStartMs: Long? = null,
)

data class BleUiState(
    val stage: BleStage = BleStage.IDLE,
    val telemetry: Esp32Telemetry? = null,
    val message: String? = null,
    val internetAvailable: Boolean? = null,
    val phoneLat: Double? = null,
    val phoneLon: Double? = null,
    val sosDeliveryState: SosDeliveryState = SosDeliveryState.NONE,
    val pendingSosCount: Int = 0,
)

data class MissionMeta(
    val title: String = DemoConfig.MISSION_NAME,
    val description: String = "",
    val category: String = "",
    val code: String = DemoConfig.MISSION_ID,
    val status: String = "active",
    val createdBy: String = DemoConfig.RESPONSIBLE_ID,
    val createdByName: String = "",
    val createdAt: Long = 0L,
    val startedAt: Long = 0L,
    val finishedAt: Long = 0L,
    val lat: Double? = null,
    val lon: Double? = null,
)

data class MissionMember(
    val rescuerId: String = DemoConfig.RESCUER_ID,
    val name: String = DemoConfig.RESCUER_NAME,
    val status: String = "Aman",
    val riskScore: Int? = null,
    val riskStatus: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val updatedAt: Long? = null,
)

enum class SosDeliveryState { NONE, SENDING, SENT, QUEUED, FAILED }

data class SosEvent(
    val eventId: String? = null,
    val missionId: String = DemoConfig.MISSION_ID,
    val rescuerId: String = DemoConfig.RESCUER_ID,
    val rescuerName: String = DemoConfig.RESCUER_NAME,
    val deviceId: String = DemoConfig.DEVICE_ID,
    val status: String = "DARURAT",
    val source: String = "unknown",
    val lat: Double? = null,
    val lon: Double? = null,
    val createdAt: Long = 0L,
    val sosPacketTimestamp: Long? = null,
)

data class GarminHealth(
    val rescuerId: String = DemoConfig.RESCUER_ID,
    val heartRate: Int? = null,
    val spo2: Int? = null,
    val stress: Int? = null,
    val bodyBattery: Int? = null,
    val respiration: Int? = null,
    val battery: Int? = null,
    val updatedAt: Long? = null,
    val source: String? = null,
)

data class Esp32UiState(
    val telemetry: Esp32Telemetry? = null,
    val sosEvents: List<SosEvent> = emptyList(),
    val garmin: GarminHealth? = null,
    val activeMissionCode: String? = null,
    val missionMeta: MissionMeta? = null,
    val members: List<MissionMember> = emptyList(),
    val missionHistory: List<MissionMeta> = emptyList(),
    val missionMessage: String? = null,
    val missionError: String? = null,
)

enum class RiskStatus(val label: String) {
    AMAN("Aman"),
    WASPADA("Waspada"),
    BAHAYA("Bahaya"),
    DARURAT("Darurat"),
}

data class RiskAssessment(
    val score: Int,
    val status: RiskStatus,
    val reason: String,
)