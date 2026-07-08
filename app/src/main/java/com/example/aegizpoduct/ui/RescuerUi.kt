package com.example.aegizpoduct.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.example.aegizpoduct.R
import com.example.aegizpoduct.logic.evaluateRisk
import com.example.aegizpoduct.model.BleStage
import com.example.aegizpoduct.model.BleUiState
import com.example.aegizpoduct.model.DemoConfig
import com.example.aegizpoduct.model.Esp32UiState
import com.example.aegizpoduct.model.GarminHealth
import com.example.aegizpoduct.model.MissionMeta
import com.example.aegizpoduct.model.MissionMember
import com.example.aegizpoduct.model.SosDeliveryState
import com.example.aegizpoduct.model.SosEvent
import com.example.aegizpoduct.session.AppSession


@Composable
fun AegizRescuerApp(
    state: BleUiState,
    firebaseState: Esp32UiState,
    onScan: () -> Unit,
    onDisconnect: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onSos: () -> Unit,
    onJoinMission: (String) -> Unit,
    onChangeRole: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showSosSentDialog by remember { mutableStateOf(false) }

    val handleSosClick = {
        onSos()
        showSosSentDialog = true
    }

    if (showSosSentDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSosSentDialog = false },
            title = {
                Text(
                    text = "SOS Terkirim",
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    color = AegizColors.Text
                )
            },
            text = {
                Text(
                    text = "Pesan darurat Anda telah berhasil dikirim ke Posko dan tim penyelamat.",
                    fontFamily = PlusJakartaSans,
                    color = AegizColors.Muted
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSosSentDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AegizColors.Red)
                ) {
                    Text("OK", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = AegizColors.Surface
        )
    }

    AegizShell(selected = tab, onSelected = { tab = it }) { padding ->
        when (tab) {
            0 -> RescuerDashboardBody(
                health = firebaseState.garmin,
                state = state,
                padding = padding,
                missionHistory = firebaseState.missionHistory,
                onViewAllHistory = { tab = 1 },
            )
            1 -> HistoryBody(padding, firebaseState.missionHistory)
            2 -> RescuerMissionBody(state = state, firebaseState = firebaseState, onJoinMission = onJoinMission, onSos = handleSosClick, padding = padding)
            else -> ProfileBody(
                roleLabel = "Rescuer",
                primaryStatus = if (state.stage == BleStage.CONNECTED) "BLE kalung tersambung" else "BLE kalung belum tersambung",
                secondaryStatus = state.message ?: "Pantau koneksi perangkat sebelum masuk misi.",
                state = state,
                health = firebaseState.garmin,
                targetDeviceLabel = "Rescuer01",
                onScan = onScan,
                onDisconnect = onDisconnect,
                onEnableBluetooth = onEnableBluetooth,
                onSos = handleSosClick,
                onChangeRole = onChangeRole,
                padding = padding,
            )
        }
    }
}


@Composable
private fun RescuerDashboardBody(
    health: GarminHealth?,
    state: BleUiState,
    padding: PaddingValues,
    missionHistory: List<MissionMeta>,
    onViewAllHistory: () -> Unit,
) {
    val heartRate = health?.heartRate?.toString() ?: "--"
    val bodyBattery = health?.bodyBattery?.toString() ?: "--"
    val stress = health?.stress?.toString() ?: "--"
    val respiration = health?.respiration?.toString() ?: "--"
    val spo2 = health?.spo2?.toString() ?: "--"
    val panicActive = state.sosDeliveryState == SosDeliveryState.SENDING ||
            state.sosDeliveryState == SosDeliveryState.SENT ||
            state.sosDeliveryState == SosDeliveryState.QUEUED
    val risk = evaluateRisk(health = health, panicActive = panicActive)

    AegizScrollContent(padding = padding, top = 38.dp) {
        DashboardHeader(greeting = "Halo, ${AppSession.currentRescuerName()}", subtitle = "Jaga dirimu tetap aman.")
        SectionTitle("Analytics")
        ReadinessScoreCard(assessment = risk)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            HealthMetricCard("Heart Rate", heartRate, if (health?.heartRate != null) heartRateStatus(health.heartRate) else "-", unit = if (health?.heartRate != null) "Bpm" else null, modifier = Modifier.weight(1f), large = true)
            HealthMetricCard("Body Battery", if (health?.bodyBattery != null) "$bodyBattery%" else "--", if (health?.bodyBattery != null) bodyBatteryStatus(health.bodyBattery) else "-", modifier = Modifier.weight(1f), large = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            HealthMetricCard("Stress Level", stress, if (health?.stress != null) stressStatus(health.stress) else "-", modifier = Modifier.weight(1f))
            HealthMetricCard("Laju Napas", respiration, if (health?.respiration != null) respirationStatus(health.respiration) else "-", unit = if (health?.respiration != null) "Rpm" else null, modifier = Modifier.weight(1f))
            HealthMetricCard("SpO₂", if (health?.spo2 != null) "$spo2%" else "--", if (health?.spo2 != null) spo2Status(health.spo2) else "-", modifier = Modifier.weight(1f))
        }


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Riwayat Misi",
                color = AegizColors.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans
            )
            Text(
                text = "Lihat Semua",
                color = AegizColors.Red,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.clickable { onViewAllHistory() }
            )
        }

        if (missionHistory.isEmpty()) {
            ActivityRow(
                icon = Icons.Default.CheckCircle,
                title = "Belum Ada Riwayat",
                subtitle = "Daftar misi yang sudah selesai akan muncul di sini",
                color = AegizColors.Muted,
            )
        } else {
            missionHistory.take(5).forEach { mission ->
                val isActive = mission.status.equals("active", ignoreCase = true)
                ActivityRow(
                    icon = if (isActive) Icons.Default.Warning else Icons.Default.CheckCircle,
                    title = mission.title,
                    subtitle = "${mission.category} • ${if (isActive) "Aktif" else "Selesai"} (${mission.code})",
                    color = if (isActive) AegizColors.Red else AegizColors.Green,
                )
            }
        }
    }
}

private fun heartRateStatus(value: Int): String = when {
    value in 60..100 -> "Normal"
    value in 101..140 -> "Meningkat"
    value in 141..170 -> "Tinggi"
    value in 40..59 || value in 171..190 -> "Waspada"
    else -> "Kritis"
}

private fun bodyBatteryStatus(value: Int): String = when {
    value > 80 -> "Optimal"
    value >= 61 -> "Baik"
    value >= 41 -> "Menurun"
    value >= 21 -> "Rendah"
    else -> "Kritis"
}

private fun stressStatus(value: Int): String = when {
    value <= 25 -> "Rendah"
    value <= 50 -> "Sedang"
    value <= 75 -> "Tinggi"
    value <= 90 -> "Waspada"
    else -> "Kritis"
}

private fun respirationStatus(value: Int): String = when {
    value in 12..20 -> "Normal"
    value in 21..24 -> "Meningkat"
    value in 25..29 -> "Tinggi"
    value in 8..11 || value in 30..34 -> "Waspada"
    else -> "Kritis"
}

private fun spo2Status(value: Int): String = when {
    value >= 95 -> "Normal"
    value >= 90 -> "Waspada"
    else -> "Kritis"
}


@Composable
private fun RescuerMissionBody(
    state: BleUiState,
    firebaseState: Esp32UiState,
    onJoinMission: (String) -> Unit,
    onSos: () -> Unit,
    padding: PaddingValues,
) {
    val tel = state.telemetry
    val gpsValue = when {
        tel?.gpsValid == true -> "FIX"
        (tel?.charsProcessed ?: 11L) < 10L -> "CEK"
        else -> "NOFIX"
    }
    val loraValue = when {
        state.stage != BleStage.CONNECTED -> "-"
        tel?.loraOk == true -> "SIAP"
        tel?.loraOk == false -> "CEK"
        else -> "-"
    }
    val myId = AppSession.currentRescuerId()
    val myMember = firebaseState.members.firstOrNull { it.rescuerId == myId }
    val isSosSentOrSending = state.sosDeliveryState == SosDeliveryState.SENDING ||
            state.sosDeliveryState == SosDeliveryState.SENT ||
            state.sosDeliveryState == SosDeliveryState.QUEUED
    val isSosActive = isSosSentOrSending || firebaseState.sosEvents.any {
        it.rescuerId == myId && it.status.equals("DARURAT", ignoreCase = true)
    }
    val statusAnda = if (isSosActive) {
        "Bahaya"
    } else {
        myMember?.status ?: "Aman"
    }
    val sosQueueValue = when {
        state.pendingSosCount > 0 -> "${state.pendingSosCount}"
        state.sosDeliveryState == SosDeliveryState.SENDING -> "KIRIM"
        state.sosDeliveryState == SosDeliveryState.SENT -> "OK"
        else -> "-"
    }
    AegizScrollContent(padding = padding, top = 54.dp) {
        CenterTitle("Misi")
        if (firebaseState.activeMissionCode == null) {
            SectionTitle("Gabung Misi")
            JoinMissionCard(message = firebaseState.missionMessage, error = firebaseState.missionError, onJoinMission = onJoinMission)
        } else {
            val context = LocalContext.current
            val lat = state.phoneLat ?: state.telemetry?.lat ?: -7.795580
            val lng = state.phoneLon ?: state.telemetry?.lng ?: 110.369490
            val hasFix = (state.phoneLat != null && state.phoneLon != null) || (state.telemetry?.gpsValid == true)

            SectionTitle("Misi Berlangsung")
            MissionDetailCard(
                mission = firebaseState.missionMeta,
                totalMembers = firebaseState.members.size.coerceAtLeast(1),
                actionLabel = "SOS Darurat",
                actionColor = AegizColors.Red,
                onAction = onSos,
                stats = listOf(
                    "BLE Kalung ESP32" to if (state.stage == BleStage.CONNECTED) "Terkoneksi" else "Menunggu",
                    "Garmin Watch" to if (firebaseState.garmin != null) "Live Sync" else "Offline",
                    "GPS Kalung" to gpsValue,
                    "LoRa SOS" to loraValue,
                    "Status Anda" to statusAnda,
                ),
            )
            MissionFeedback(firebaseState.missionMessage, firebaseState.missionError)

            SectionTitle("Peta & Koordinat SAR")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AegizColors.Surface),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = AegizColors.Red, modifier = Modifier.size(20.dp))
                            Text("Posisi Rescuer 01", color = AegizColors.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = PlusJakartaSans)
                        }
                        Text(if (hasFix) "FIX LIVE" else "DEMO GPS", color = if (hasFix) AegizColors.Green else Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AegizColors.Background),
                    ) {
                        Esp32Map(lat = lat, lng = lng, modifier = Modifier.fillMaxSize())
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(24.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = AegizColors.Red.copy(alpha = 0.3f),
                                border = androidx.compose.foundation.BorderStroke(2.dp, AegizColors.Red)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Surface(modifier = Modifier.size(8.dp), shape = androidx.compose.foundation.shape.CircleShape, color = AegizColors.Red) {}
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "%.6f, %.6f".format(Locale.US, lat, lng),
                            color = AegizColors.Text,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Button(
                            onClick = {
                                val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Rescuer 01)")
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AegizColors.Background, contentColor = AegizColors.Text),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = AegizColors.Red, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Google Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            SectionTitle("Pengingat Operasional & Keselamatan")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AegizColors.Surface),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = AegizColors.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Prosedur Keselamatan SAR",
                            color = AegizColors.Text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = PlusJakartaSans
                        )
                    }
                    Text(
                        "• Selalu periksa koneksi kalung LoRa & Bluetooth sebelum memasuki area blank spot.\n" +
                                "• Pantau indikator Heart Rate & Body Battery Anda secara berkala pada Garmin.\n" +
                                "• Tekan tombol SOS Darurat hanya saat menghadapi bahaya atau cedera serius.\n" +
                                "• Tetap berkoordinasi dengan Pengawas/Posko dan laporkan situasi cuaca lapangan.",
                        color = AegizColors.Muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontFamily = PlusJakartaSans
                    )
                }
            }
        }
    }
}

@Composable
private fun JoinMissionCard(message: String?, error: String?, onJoinMission: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    val focusRequester = remember { FocusRequester() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AegizColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Masukkan kode misi 6 digit",
                color = AegizColors.Text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans
            )
            Text(
                text = "Ketuk kotak di bawah ini untuk mulai mengetik kode misi",
                color = AegizColors.Muted,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { focusRequester.requestFocus() }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val padded = code.padEnd(6, ' ')
                    for (i in 0..5) {
                        val digit = padded[i]
                        val isFilled = i < code.length
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            color = if (isFilled) AegizColors.Red.copy(alpha = 0.08f) else AegizColors.Background,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (i == code.length) 2.dp else 1.dp,
                                color = if (i == code.length) AegizColors.Red else AegizColors.Outline
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (digit == ' ') "" else digit.toString(),
                                    color = AegizColors.Text,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                BasicTextField(
                    value = code,
                    onValueChange = {
                        code = it.filter(Char::isDigit).take(6)
                        localError = null
                    },
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            MissionFeedback(message = message, error = localError ?: error)

            Button(
                onClick = {
                    if (code.length != 6) {
                        localError = "Kode misi harus 6 digit"
                    } else {
                        onJoinMission(code)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AegizColors.Red,
                    contentColor = Color.White
                ),
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Gabung Misi", fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            }
        }
    }
}