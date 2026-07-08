package com.example.aegizpoduct.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.CameraAlt
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.aegizpoduct.MainActivity
import com.example.aegizpoduct.R
import com.example.aegizpoduct.logic.login
import com.example.aegizpoduct.logic.FirebaseRestClient
import com.example.aegizpoduct.logic.pollMembers
import androidx.compose.material.icons.filled.History
import com.example.aegizpoduct.model.AppRole
import com.example.aegizpoduct.model.BleStage
import com.example.aegizpoduct.model.BleUiState
import com.example.aegizpoduct.model.MissionMember
import com.example.aegizpoduct.model.GarminHealth
import com.example.aegizpoduct.model.MissionMeta
import com.example.aegizpoduct.model.RiskAssessment
import com.example.aegizpoduct.model.RiskStatus
import com.example.aegizpoduct.model.SosDeliveryState
import com.example.aegizpoduct.model.SosEvent
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

internal val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold), // Bobot semi-tebal
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold), // Bobot tebal
)


internal object AegizColors {
    val Background = Color(0xFFEFF2F6)
    val Surface = Color.White
    val Text = Color(0xFF121212)
    val Muted = Color(0xFF8F8F8F)
    val Inactive = Color(0xFFAFAFAF)
    val Red = Color(0xFFC90D03)
    val RedDeep = Color(0xFFB80000)
    val RedBright = Color(0xFFF12A21)
    val DangerSoft = Color(0xFFEE8B87)
    val Green = Color(0xFF13B900)
    val GreenSoft = Color(0xFFC8E6C9)
    val Orange = Color(0xFFFF9800)
    val DarkCard = Color(0xFF292929)
    val Outline = Color(0xFFD0D7DE)
    val OutlineSoft = Color(0xFFE1E4E8)
}

fun riskColor(status: RiskStatus): Color = when (status) {
    RiskStatus.AMAN -> AegizColors.Green
    RiskStatus.WASPADA -> Color(0xFFFFB300)
    RiskStatus.BAHAYA -> Color(0xFFF57C00)
    RiskStatus.DARURAT -> AegizColors.Red
}

fun riskTextColor(status: String): Color = when {
    status.equals(RiskStatus.DARURAT.label, ignoreCase = true) -> AegizColors.Red
    status.equals(RiskStatus.BAHAYA.label, ignoreCase = true) -> Color(0xFFF57C00)
    status.equals(RiskStatus.WASPADA.label, ignoreCase = true) -> Color(0xFFFF9800)
    else -> AegizColors.Green
}

fun String.isAlertStatus(): Boolean =
    equals(RiskStatus.DARURAT.label, ignoreCase = true) ||
            equals(RiskStatus.BAHAYA.label, ignoreCase = true)

fun formatMemberLocation(member: MissionMember): String? {
    val lat = member.lat
    val lon = member.lon
    return if (lat != null && lon != null) "%.4f, %.4f".format(Locale.US, lat, lon) else null
}

fun formatEventLocation(event: SosEvent): String? {
    val lat = event.lat
    val lon = event.lon
    return if (lat != null && lon != null) "%.4f, %.4f".format(Locale.US, lat, lon) else null
}


@Composable
fun AegizShell(
    selected: Int,
    onSelected: (Int) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = AegizColors.Background,
        bottomBar = { AegizBottomNav(selected = selected, onSelected = onSelected) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(AegizColors.Background)) {
            content(padding)
        }
    }
}

@Composable
private fun AegizBottomNav(selected: Int, onSelected: (Int) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = AegizColors.Surface, shadowElevation = 0.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(88.dp).padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val items = listOf(Icons.Default.Home, Icons.Default.History, Icons.AutoMirrored.Filled.Input, Icons.Default.Person)
            items.forEachIndexed { index, icon ->
                IconButton(onClick = { onSelected(index) }) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (selected == index) AegizColors.Red else AegizColors.Inactive,
                        modifier = Modifier.size(if (index == 2) 35.dp else 32.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun AegizScrollContent(
    padding: PaddingValues,
    top: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AegizColors.Background)
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
            .padding(top = top, bottom = 108.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
fun DashboardHeader(greeting: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(greeting, color = AegizColors.Text, fontSize = 21.sp, lineHeight = 24.sp)
            Text(subtitle, color = AegizColors.Red, fontSize = 15.sp, lineHeight = 21.sp)
        }
        Box {
            Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = AegizColors.Text, modifier = Modifier.size(32.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 3.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE33A31)),
            )
        }
    }
}

@Composable
fun CenterTitle(text: String) {
    Text(text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = AegizColors.Text, fontSize = 21.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = AegizColors.Text, fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
fun SectionHeaderWithAction(title: String, action: String = "Lihat Semua") {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        SectionTitle(title)
        Text(action, color = AegizColors.DangerSoft, fontSize = 13.sp, lineHeight = 16.sp)
    }
}


@Composable
fun AegizAlertStrip(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AegizColors.DangerSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 19.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
fun AegizDeviceStatusCard(title: String, state: BleUiState, receiverMode: Boolean) {
    val tel = state.telemetry
    val connected = state.stage == BleStage.CONNECTED
    val gpsText = when {
        receiverMode && tel?.gpsValid == true && tel.lat != null && tel.lng != null -> "%.5f, %.5f".format(Locale.US, tel.lat, tel.lng)
        receiverMode -> "Menunggu paket LoRa"
        tel?.gpsValid == true && tel.lat != null && tel.lng != null -> "FIX %.5f, %.5f".format(Locale.US, tel.lat, tel.lng)
        (tel?.charsProcessed ?: 11L) < 10L -> "Cek GPS TX GPIO34"
        else -> "Belum fix"
    }
    val gpsColor = when {
        tel?.gpsValid == true -> AegizColors.Green
        !receiverMode && (tel?.charsProcessed ?: 11L) < 10L -> AegizColors.Red
        else -> AegizColors.Muted
    }
    val loraText = when {
        tel?.loraOk == true -> if (receiverMode) "Paket diterima" else "Siap SOS"
        tel?.loraOk == false && receiverMode -> "Menunggu SOS"
        tel?.loraOk == false -> "Cek modul"
        else -> "-"
    }
    val loraColor = when {
        tel?.loraOk == true -> AegizColors.Green
        tel?.loraOk == false && receiverMode -> AegizColors.Muted
        tel?.loraOk == false -> AegizColors.Red
        else -> AegizColors.Muted
    }
    val note = if (receiverMode) {
        "Receiver tidak punya GPS lokal. LoRa baru OK setelah paket dari Rescuer01 masuk."
    } else {
        "Perangkat BLE NUS dipindai dari aplikasi; pairing manual tidak diperlukan."
    }
    val queueText = when {
        state.pendingSosCount > 0 -> "${state.pendingSosCount} antri"
        state.sosDeliveryState == SosDeliveryState.SENDING -> "Mengirim"
        state.sosDeliveryState == SosDeliveryState.SENT -> "Terkirim"
        state.sosDeliveryState == SosDeliveryState.FAILED -> "Gagal"
        else -> "-"
    }
    val queueColor = when {
        state.pendingSosCount > 0 -> AegizColors.Red
        state.sosDeliveryState == SosDeliveryState.SENDING -> AegizColors.Muted
        state.sosDeliveryState == SosDeliveryState.SENT -> AegizColors.Green
        state.sosDeliveryState == SosDeliveryState.FAILED -> AegizColors.Red
        else -> AegizColors.Muted
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AegizColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = if (connected) AegizColors.Green else AegizColors.Red)
                Spacer(Modifier.width(8.dp))
                Text(title, color = AegizColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(
                    color = if (connected) AegizColors.GreenSoft else AegizColors.DangerSoft,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        if (connected) "ESP32 TERHUBUNG" else "BELUM TERHUBUNG",
                        color = if (connected) AegizColors.Green else AegizColors.Red,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            AegizStatusLine("BLE", if (connected) "Tersambung" else "Belum tersambung", if (connected) AegizColors.Green else AegizColors.Red)
            AegizStatusLine("GPS", gpsText, gpsColor)
            AegizStatusLine(if (receiverMode) "LoRa Link" else "LoRa SOS", loraText, loraColor)
            AegizStatusLine("SOS Online", queueText, queueColor)
            Text(note, color = AegizColors.Muted, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
fun GarminDeviceStatusCard(health: GarminHealth?) {
    val connected = health != null && (health.heartRate != null || health.spo2 != null)
    val hrStr = health?.heartRate?.let { "$it Bpm" } ?: "--"
    val hrStatus = if (health?.heartRate != null) (if (health.heartRate in 60..100) "Normal" else "Meningkat") else "-"
    val hrColor = if (health?.heartRate != null && health.heartRate in 60..100) AegizColors.Green else if (health?.heartRate != null) AegizColors.Orange else AegizColors.Muted

    val spo2Str = health?.spo2?.let { "$it%" } ?: "--"
    val respStr = health?.respiration?.let { "$it Rpm" } ?: "--"
    val spo2Color = if (health?.spo2 != null && health.spo2 >= 95) AegizColors.Green else if (health?.spo2 != null) AegizColors.Orange else AegizColors.Muted

    val bbStr = health?.bodyBattery?.let { "$it%" } ?: "--"
    val bbColor = if (health?.bodyBattery != null && health.bodyBattery > 30) AegizColors.Green else if (health?.bodyBattery != null) AegizColors.Orange else AegizColors.Muted

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AegizColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = if (connected) AegizColors.Green else AegizColors.Red)
                Spacer(Modifier.width(8.dp))
                Text("Garmin Smartwatch", color = AegizColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(
                    color = if (connected) AegizColors.GreenSoft else AegizColors.DangerSoft,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        if (connected) "GARMIN TERHUBUNG" else "BELUM TERHUBUNG",
                        color = if (connected) AegizColors.Green else AegizColors.Red,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            AegizStatusLine("Status Sync", if (connected) "Live Sync (Vital Aktif)" else "Menunggu koneksi jam", if (connected) AegizColors.Green else AegizColors.Red)
            AegizStatusLine("Sensor Detak Jantung", "$hrStr ($hrStatus)", hrColor)
            AegizStatusLine("SpO₂ & Napas", "$spo2Str | $respStr", spo2Color)
            AegizStatusLine("Body Battery", bbStr, bbColor)
            Text(
                "Sensor vital Garmin terhubung secara real-time untuk memantau saturasi oksigen, detak jantung, dan menghitung Readiness Score Anda.",
                color = AegizColors.Muted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun AegizStatusLine(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AegizColors.Muted, fontSize = 13.sp, lineHeight = 16.sp)
        Text(value, color = color, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}


@Composable
fun HealthMetricCard(label: String, value: String, status: String, modifier: Modifier = Modifier, unit: String? = null, large: Boolean = false) {
    Card(
        modifier = modifier.height(117.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = AegizColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = AegizColors.Text, fontSize = 14.sp, lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, color = AegizColors.Red, fontSize = if (large) 48.sp else 39.sp, lineHeight = if (large) 48.sp else 40.sp, fontWeight = FontWeight.Bold)
                    if (unit != null) {
                        Spacer(Modifier.width(2.dp))
                        Text(unit, color = AegizColors.Red, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(bottom = 5.dp))
                    }
                }
                Text(status, color = AegizColors.Muted, fontSize = 12.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
fun ReadinessScoreCard(assessment: RiskAssessment) {
    Card(
        modifier = Modifier.fillMaxWidth().height(117.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = riskColor(assessment.status)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("Risk Score", color = Color.White, fontSize = 16.sp, lineHeight = 20.sp)
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(assessment.reason + "\nStatus: " + assessment.status.label, color = Color.White, fontSize = 14.sp, lineHeight = 18.sp)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(assessment.score.toString(), color = Color.White, fontSize = 64.sp, lineHeight = 66.sp)
                Text(assessment.status.label, color = Color.White, fontSize = 14.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
fun MissionSummaryCard(mission: MissionMeta?, members: List<MissionMember>, onClick: () -> Unit) {
    val title = mission?.title?.takeIf { it.isNotBlank() } ?: com.example.aegizpoduct.model.DemoConfig.MISSION_NAME
    val description = mission?.description?.takeIf { it.isNotBlank() } ?: "Operasi SAR aktif"
    val code = mission?.code ?: "-"
    Box(modifier = Modifier.fillMaxWidth().height(162.dp)) {
        SirenArt(modifier = Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-58).dp))
        Card(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(117.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = AegizColors.Red),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(AegizColors.RedBright, AegizColors.RedDeep)))) {
                Column(modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Misi :", color = Color.White, fontSize = 15.sp, lineHeight = 18.sp)
                    Text(title, color = Color.White, fontSize = 21.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Deskripsi : $description", color = Color.White, fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Kode : $code | Anggota : ${members.size}", color = Color.White, fontSize = 14.sp, lineHeight = 17.sp)
                }
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 12.dp).clickable(onClick = onClick),
                    color = Color.White,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text("Lihat Selengkapnya >", modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), color = AegizColors.Text, fontSize = 13.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmptyMissionPrompt(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = AegizColors.Surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.Input, contentDescription = null, tint = AegizColors.Red, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Belum Ada Misi Saat Ini", color = AegizColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Buat misi baru dari tab Misi.", color = AegizColors.Muted, fontSize = 13.sp)
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AegizColors.Text)
            }
        }
    }
}

@Composable
fun MissionDetailCard(
    mission: MissionMeta?,
    totalMembers: Int,
    actionLabel: String,
    actionColor: Color,
    onAction: () -> Unit,
    stats: List<Pair<String, String>>,
) {
    val title = mission?.title?.takeIf { it.isNotBlank() } ?: com.example.aegizpoduct.model.DemoConfig.MISSION_NAME
    val description = mission?.description?.takeIf { it.isNotBlank() } ?: "Operasi SAR aktif"
    val category = mission?.category?.takeIf { it.isNotBlank() } ?: "Umum"
    val code = mission?.code ?: "-"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AegizColors.Red),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(AegizColors.RedBright, AegizColors.RedDeep))).padding(12.dp)) {
            SirenArt(modifier = Modifier.align(Alignment.TopEnd).offset(x = 16.dp, y = (-24).dp), width = 126.dp)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Misi :", color = Color.White, fontSize = 15.sp, lineHeight = 18.sp)
                    Text(title, color = Color.White, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Deskripsi : $description", color = Color.White, fontSize = 16.sp, lineHeight = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Kategori : $category", color = Color.White, fontSize = 16.sp, lineHeight = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Kode : $code | Total Anggota : $totalMembers", color = Color.White, fontSize = 16.sp, lineHeight = 20.sp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    stats.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { stat -> MissionStatCard(label = stat.first, value = stat.second, modifier = Modifier.weight(1f)) }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = actionColor, contentColor = Color.White),
                ) {
                    Text(actionLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MissionStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(87.dp), color = AegizColors.Surface, shape = RoundedCornerShape(10.dp)) {
        Box(Modifier.fillMaxSize().padding(8.dp)) {
            Text(label, color = AegizColors.Text, fontSize = 13.sp, lineHeight = 16.sp, maxLines = 2)
            Text(
                value,
                modifier = Modifier.align(Alignment.BottomEnd),
                color = AegizColors.Red,
                fontSize = if (value.length > 3) 27.sp else 49.sp,
                lineHeight = if (value.length > 3) 32.sp else 52.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun MissionFeedback(message: String?, error: String?, compact: Boolean = false) {
    val text = error ?: message ?: return
    val color = if (error != null) AegizColors.Red else AegizColors.Green
    Text(
        text,
        color = if (compact) Color.White else color,
        fontSize = if (compact) 12.sp else 13.sp,
        lineHeight = if (compact) 15.sp else 17.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = if (compact) 2 else 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun EmptyRosterCard() {
    Surface(modifier = Modifier.fillMaxWidth().height(64.dp), color = AegizColors.Surface, shape = RoundedCornerShape(10.dp)) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, tint = AegizColors.Muted)
            Spacer(Modifier.width(12.dp))
            Text("Belum ada rescuer yang tergabung", color = AegizColors.Muted, fontSize = 14.sp)
        }
    }
}

@Composable
fun LiveSosMapCard(event: SosEvent, member: MissionMember?) {
    val lat = event.lat ?: return
    val lon = event.lon ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AegizColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Esp32Map(lat = lat, lng = lon, modifier = Modifier.fillMaxWidth().height(335.dp).clip(RoundedCornerShape(10.dp)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = AegizColors.Red)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(member?.name ?: event.rescuerName, color = AegizColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Status ${member?.status ?: event.status} | Risk ${member?.riskScore ?: "-"}",
                        color = riskTextColor(member?.status ?: event.status),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("%.5f, %.5f".format(Locale.US, lat, lon), color = AegizColors.Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun MemberRow(member: MissionMember, onResolved: (() -> Unit)? = null) {
    val alert = member.status.isAlertStatus()
    val location = formatMemberLocation(member)
    val riskText = member.riskScore?.toString() ?: "-"

    if (alert) {
        // === ALERT STATE (coral/merah muda) — sesuai Figma ===
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AegizColors.DangerSoft,
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Baris atas: avatar + nama/status | lokasi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Image(
                        painter = painterResource(id = R.drawable.avatar_hasan),
                        contentDescription = "Avatar ${member.name}",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    // Nama + Status
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans
                        )
                        Text(
                            text = member.status,
                            color = AegizColors.Red,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // Lokasi
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Lokasi Terakhir :",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = PlusJakartaSans
                        )
                        Text(
                            text = location ?: "Tidak Diketahui",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans
                        )
                    }
                }

                // Baris bawah: Condition Score | Tombol Tertolong
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Risk Score :",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = PlusJakartaSans
                        )
                        Text(
                            text = riskText,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans
                        )
                    }
                    Button(
                        onClick = { onResolved?.invoke() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AegizColors.Green,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Tertolong",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans
                        )
                    }
                }
            }
        }
    } else {
        // === NORMAL STATE (putih bersih) — tampilkan nama, status berwarna, risk score ===
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AegizColors.Surface,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Image(
                    painter = painterResource(id = R.drawable.avatar_hasan),
                    contentDescription = "Avatar ${member.name}",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(14.dp))
                // Nama + Status berwarna + Risk Score
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        color = AegizColors.Text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans
                    )
                    Text(
                        text = member.status,
                        color = riskTextColor(member.status),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AegizColors.Muted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun Avatar(size: Dp = 40.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF4C5B62), Color(0xFF111417)))),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = size * 0.13f)
                .width(size * 0.55f)
                .height(size * 0.22f)
                .clip(RoundedCornerShape(topStart = size * 0.2f, topEnd = size * 0.2f))
                .background(Color(0xFFDADADA)),
        )
        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.62f))
    }
}

/**
 * Composable foto profil interaktif:
 * - Jika ada foto profil (dari galeri), tampilkan fotonya
 * - Jika tidak ada, tampilkan avatar default
 * - Badge kamera di pojok kanan bawah untuk memilih foto baru
 * - Tombol hapus di bawah avatar (hanya muncul jika ada foto)
 */
@Composable
fun ProfilePhotoSection(size: Dp = 60.dp) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val profilePhotoPath by com.example.aegizpoduct.session.AppSession.profilePhotoUri.collectAsState()

    // Launcher untuk membuka galeri gambar
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            // Salin file gambar ke folder internal profiles/ agar persist
            val destDir = File(context.filesDir, "profiles").also { it.mkdirs() }
            val uid = com.example.aegizpoduct.session.AppSession.uid.value ?: "default"
            val destFile = File(destDir, "$uid.jpg")
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            com.example.aegizpoduct.session.AppSession.setProfilePhoto(destFile.absolutePath)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.BottomEnd) {
            // Foto atau avatar default
            if (profilePhotoPath != null) {
                AsyncImage(
                    model = File(profilePhotoPath!!),
                    contentDescription = "Foto Profil",
                    modifier = Modifier.size(size).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.avatar_hasan),
                )
            } else {
                Avatar(size = size)
            }
            // Badge kamera di pojok kanan bawah
            Surface(
                modifier = Modifier
                    .size(size * 0.35f)
                    .clip(CircleShape)
                    .clickable { launcher.launch("image/*") },
                color = AegizColors.Red,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Ganti Foto",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.18f)
                    )
                }
            }
        }
        // Tombol hapus foto (hanya tampil jika ada foto)
        if (profilePhotoPath != null) {
            TextButton(
                onClick = { com.example.aegizpoduct.session.AppSession.clearProfilePhoto() },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Hapus Foto",
                    color = AegizColors.Red,
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans
                )
            }
        }
    }
}

@Composable
fun SirenArt(modifier: Modifier = Modifier, width: Dp = 118.dp) {
    Box(modifier = modifier.width(width).height(width * 0.82f), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).width(width * 0.66f).height(width * 0.16f)
                .clip(RoundedCornerShape(4.dp)).background(Brush.verticalGradient(listOf(Color(0xFF343434), Color(0xFF111111)))),
        )
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = width * 0.10f).width(width * 0.55f).height(width * 0.52f)
                .clip(RoundedCornerShape(topStart = width * 0.2f, topEnd = width * 0.2f, bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFF8A83), Color(0xFFE20600), Color(0xFF8D0300)))),
        )
        Box(modifier = Modifier.align(Alignment.Center).width(width * 0.28f).height(width * 0.46f).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.24f)))
        Box(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = width * 0.26f).width(width * 0.46f).height(width * 0.08f)
                .clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.22f)),
        )
    }
}

@Composable
fun ProfileBody(
    roleLabel: String, // Label peran pengguna (misal: "Rescuer" atau "Penanggung Jawab")
    primaryStatus: String,
    secondaryStatus: String,
    state: BleUiState,
    health: GarminHealth?,   // data vital dari Garmin (null jika tidak tersedia)
    targetDeviceLabel: String,
    onScan: () -> Unit,
    onDisconnect: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onSos: (() -> Unit)?,
    onChangeRole: () -> Unit,
    padding: PaddingValues,
) {
    val session = com.example.aegizpoduct.session.AppSession
    // Ambil nama lengkap pengguna dari sesi aktif via currentRescuerName() (fallback ke roleLabel)
    val displayName = session.currentRescuerName().takeIf { it.isNotBlank() && it != com.example.aegizpoduct.model.DemoConfig.RESCUER_NAME } ?: roleLabel
    // Ambil email pengguna dari sesi aktif via StateFlow
    val emailValue = session.email.value

    AegizScrollContent(padding = padding, top = 54.dp) {
        // Judul halaman Profil di tengah
        CenterTitle("Profil")

        // Card utama profil pengguna: menampilkan nama dan peran
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AegizColors.Surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Foto profil interaktif dengan tombol kamera dan opsi hapus
                    ProfilePhotoSection(size = 60.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Nama profil pengguna ditampilkan sebagai teks utama yang besar dan tebal
                        Text(
                            text = displayName,
                            color = AegizColors.Text,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                            lineHeight = 24.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        // Peran pengguna ditampilkan di bawah nama (misal: Rescuer / Penanggung Jawab)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = AegizColors.Red.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = roleLabel, // Menampilkan label peran dari parameter
                                    color = AegizColors.Red,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSans,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            // Indikator status koneksi (hijau jika BLE tersambung)
                            Surface(
                                color = if (state.stage == BleStage.CONNECTED) AegizColors.GreenSoft else AegizColors.DangerSoft,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (state.stage == BleStage.CONNECTED) "Online" else "Offline",
                                    color = if (state.stage == BleStage.CONNECTED) AegizColors.Green else AegizColors.Red,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = PlusJakartaSans,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
                // Informasi tambahan: email pengguna dari sesi aktif
                if (emailValue != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AegizColors.Muted, modifier = Modifier.size(16.dp))
                        Text(
                            text = emailValue,
                            color = AegizColors.Muted,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans
                        )
                    }
                }
                // Status deskriptif koneksi perangkat (misalnya: pesan dari BLE)
                Text(
                    text = secondaryStatus,
                    color = AegizColors.Muted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontFamily = PlusJakartaSans
                )
            }
        }

        // Card status perangkat BLE (Bluetooth & LoRa)
        AegizDeviceStatusCard(title = targetDeviceLabel, state = state, receiverMode = targetDeviceLabel.contains("Penanggung", ignoreCase = true))

        // Card status Garmin (hanya ditampilkan jika data health tersedia atau rolenya adalah Rescuer)
        // null artinya tidak ditampilkan (misal: untuk role Pengawas/Posko)
        if (health != null || roleLabel.equals("Rescuer", ignoreCase = true)) {
            GarminDeviceStatusCard(health = health)
        }

        // Tombol-tombol aksi perangkat
        Button(
            onClick = onScan,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AegizColors.Red, contentColor = Color.White),
        ) {
            Icon(Icons.Default.Bluetooth, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Scan $targetDeviceLabel", fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        }
        OutlinedButton(
            onClick = onEnableBluetooth,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, AegizColors.Red),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = AegizColors.Red)
            Spacer(Modifier.width(8.dp))
            Text("Nyalakan Bluetooth", color = AegizColors.Red, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        }
        OutlinedButton(
            onClick = onDisconnect,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, AegizColors.Inactive),
        ) {
            Text("Putuskan Koneksi", color = AegizColors.Text, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        }
        if (onSos != null) {
            Button(
                onClick = onSos,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AegizColors.DangerSoft, contentColor = Color.White),
            ) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("SOS Darurat", fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onChangeRole,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F), contentColor = Color.White),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("GANTI PERAN (RESCUER / POSKO)", fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
fun ActivityList(title: String, showAll: Boolean = true) {
    SectionHeaderWithAction(title = title)
}

@Composable
fun ActivityRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)? = null, // optional: jika ada, baris ini bisa diklik
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AegizColors.Surface),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = AegizColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = AegizColors.Muted, fontSize = 12.sp)
            }
            // Tampilkan ikon panah kanan jika item bisa diklik
            if (onClick != null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AegizColors.Muted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun HistoryBody(padding: PaddingValues, history: List<MissionMeta> = emptyList()) {
    // State: misi yang sedang dipilih untuk ditampilkan detailnya
    // null = tampilkan daftar riwayat, non-null = tampilkan halaman detail misi
    var selectedMission by remember { mutableStateOf<MissionMeta?>(null) }

    if (selectedMission != null) {
        // Tampilkan halaman detail misi ketika user mengklik salah satu item
        MissionDetailPage(
            mission = selectedMission!!,
            padding = padding,
            onBack = { selectedMission = null }, // tombol back kembali ke daftar
        )
    } else {
        // Tampilkan daftar riwayat misi
        AegizScrollContent(padding = padding, top = 54.dp) {
            CenterTitle("Riwayat")
            SectionTitle("Riwayat Misi")
            if (history.isEmpty()) {
                ActivityRow(
                    icon = Icons.Default.CheckCircle,
                    title = "Belum Ada Riwayat",
                    subtitle = "Daftar misi yang selesai akan muncul di sini",
                    color = Color(0xFF2E7D32),
                )
            } else {
                history.forEach { mission ->
                    val isActive = mission.status.equals("active", ignoreCase = true)
                    // Setiap item bisa diklik untuk melihat detail misi
                    ActivityRow(
                        icon = if (isActive) Icons.Default.Warning else Icons.Default.CheckCircle,
                        title = mission.title,
                        subtitle = "${mission.category} • ${if (isActive) "Aktif" else "Selesai"} (${mission.code})",
                        color = if (isActive) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                        onClick = { selectedMission = mission }, // klik = pilih misi ini
                    )
                }
            }
        }
    }
}

/**
 * Halaman detail misi: menampilkan semua informasi misi yang dipilih
 * dari daftar riwayat. Ada tombol back di atas untuk kembali ke daftar.
 */
@Composable
fun MissionDetailPage(
    mission: MissionMeta,  // data misi yang akan ditampilkan
    padding: PaddingValues,
    onBack: () -> Unit,    // fungsi yang dipanggil ketika tombol back diklik
) {
    // State untuk menyimpan daftar anggota yang ikut misi ini
    var members by remember { mutableStateOf<List<MissionMember>>(emptyList()) }
    val client = remember { FirebaseRestClient() }

    // Resolusi nama pembuat misi (bukan UID)
    var creatorName by remember {
        mutableStateOf(
            mission.createdByName.takeIf { it.isNotBlank() }
                ?: com.example.aegizpoduct.model.DemoConfig.accounts.firstOrNull { it.userId == mission.createdBy }?.displayName
                ?: mission.createdBy
        )
    }

    // Memuat daftar anggota dari Firebase secara asinkron saat halaman dibuka
    LaunchedEffect(mission.code) {
        runCatching {
            pollMembers(client, mission.code)
        }.onSuccess { list ->
            members = list
        }

        // Jika nama masih berupa UID, coba ambil fullname dari database Firebase /users
        if (mission.createdByName.isBlank() && mission.createdBy.isNotBlank() && mission.createdBy == creatorName) {
            runCatching {
                com.example.aegizpoduct.logic.fetchUserFromFirebase(client, mission.createdBy)
            }.onSuccess { user ->
                if (user != null && user.fullname.isNotBlank()) {
                    creatorName = user.fullname
                }
            }
        }
    }

    // Format epoch ms ke string jam:menit tanggal
    fun formatTime(epochMs: Long): String {
        if (epochMs == 0L) return "--"
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID"))
        return sdf.format(java.util.Date(epochMs))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AegizColors.Background)
            .statusBarsPadding()
            .padding(bottom = 108.dp)
    ) {
        // Header: tombol back + judul misi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tombol back berbentuk ikon panah kiri
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = AegizColors.Text
                )
            }
            // Judul misi ditampilkan di samping tombol back
            Text(
                text = mission.title,
                color = AegizColors.Text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Konten detail misi yang bisa di-scroll
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Status badge: Aktif (merah) atau Selesai (hijau)
            val isActive = mission.status.equals("active", ignoreCase = true)
            Surface(
                color = if (isActive) AegizColors.Red.copy(alpha = 0.12f) else AegizColors.GreenSoft,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (isActive) "Aktif" else "Selesai",
                    color = if (isActive) AegizColors.Red else AegizColors.Green,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Card utama berisi semua detail misi
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AegizColors.Surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Setiap baris detail misi ditampilkan oleh MissionDetailRow
                    MissionDetailRow("Deskripsi", mission.description.ifBlank { "-" })
                    MissionDetailRow("Kategori", mission.category.ifBlank { "-" })
                    MissionDetailRow("Kode Misi", mission.code, mono = true)
                    MissionDetailRow("Dibuat Oleh", creatorName)

                    // Lokasi misi (latitude & longitude) jika tersedia
                    if (mission.lat != null && mission.lon != null) {
                        MissionDetailRow(
                            label = "Lokasi Misi",
                            value = "%.6f, %.6f".format(java.util.Locale.US, mission.lat, mission.lon),
                            mono = true
                        )
                    } else {
                        MissionDetailRow("Lokasi Misi", "Lokasi tidak tersedia")
                    }

                    // Jam mulai misi
                    MissionDetailRow(
                        label = "Jam Mulai",
                        // startedAt disimpan dalam epoch ms; format ke string waktu lokal
                        value = formatTime(if (mission.startedAt > 0) mission.startedAt else mission.createdAt * 1000)
                    )

                    // Jam selesai misi (jika sudah selesai)
                    MissionDetailRow(
                        label = "Jam Selesai",
                        value = if (mission.finishedAt > 0) formatTime(mission.finishedAt) else "Belum selesai"
                    )
                }
            }

            // List Anggota yang ikut misi ini (selalu menampilkan list card anggota yang ikut)
            SectionTitle("Anggota Yang Terhubung")
            if (members.isEmpty()) {
                EmptyRosterCard()
            } else {
                members.forEach { member ->
                    MemberRow(member = member)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Baris detail misi: label di kiri, nilai di kanan.
 * Digunakan di dalam MissionDetailPage.
 */
@Composable
fun MissionDetailRow(label: String, value: String, mono: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        // Label (misalnya "Deskripsi", "Kategori", dll.)
        Text(
            text = label,
            color = AegizColors.Muted,
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans
        )
        // Nilai dari field tersebut
        Text(
            text = value,
            color = AegizColors.Text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (mono) FontFamily.Monospace else PlusJakartaSans
        )
        // Garis pemisah di bawah setiap baris
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AegizColors.OutlineSoft)
        )
    }
}


@Composable
fun Esp32Map(lat: Double, lng: Double, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            load(context, context.getSharedPreferences("osmdroid", 0))
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            controller.setZoom(17.0)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
        }
    }
    val marker = remember { Marker(mapView).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); title = "ESP32" } }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause(); mapView.onDetach() }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { map ->
            val point = GeoPoint(lat, lng)
            marker.position = point
            marker.snippet = "%.6f, %.6f".format(lat, lng)
            if (!map.overlays.contains(marker)) map.overlays.add(marker)
            map.controller.setCenter(point)
            map.invalidate()
        },
    )
}


object SosNotifier {
    private const val CHANNEL_ID = "sos_alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "SOS Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifikasi darurat saat rescuer menekan SOS"
            },
        )
    }

    fun notifySos(context: Context, eventId: String, rescuerName: String, lat: Double?, lon: Double?) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("sos_event_id", eventId)
        }
        val pendingIntent = PendingIntent.getActivity(context, eventId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val locationText = if (lat != null && lon != null) "%.5f, %.5f".format(Locale.US, lat, lon) else "Lokasi belum tersedia"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("SOS dari $rescuerName")
            .setContentText(locationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Rescuer $rescuerName mengirim SOS. Lokasi: $locationText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(eventId.hashCode(), notification)
    }
}


@Composable
fun RoleScreen(onLogin: (AppRole) -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(modifier = Modifier.size(72.dp), shape = RoundedCornerShape(6.dp), color = OpsColors.Panel, border = BorderStroke(1.dp, OpsColors.Outline)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Security, contentDescription = null, tint = OpsColors.Cyan, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Aegiz", color = OpsColors.Cyan, fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "SISTEM PEMANTAUAN SAR IOT",
            color = OpsColors.Muted,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(30.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, OpsColors.Outline),
            colors = CardDefaults.cardColors(containerColor = OpsColors.Panel),
        ) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    label = { Text("ID PENGGUNA") },
                    colors = loginFieldColors(),
                    shape = RoundedCornerShape(3.dp),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    singleLine = true,
                    label = { Text("KATA SANDI") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = loginFieldColors(),
                    shape = RoundedCornerShape(3.dp),
                )
                if (error != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = OpsColors.RescueRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(error.orEmpty(), color = OpsColors.RescueRedSoft, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(
                    onClick = {
                        val account = login(username, password)
                        if (account == null) {
                            error = "Username atau password demo tidak cocok"
                        } else {
                            onLogin(account.role)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(3.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OpsColors.Cyan, contentColor = Color.Black),
                ) {
                    Text("MASUK SISTEM", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DemoRoleButton("Rescuer", Modifier.weight(1f)) { onLogin(AppRole.RESCUER) }
                    DemoRoleButton("P. Jawab", Modifier.weight(1f)) { onLogin(AppRole.PENANGGUNG_JAWAB) }
                }
            }
        }
    }
}

@Composable
private fun DemoRoleButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(3.dp),
        border = BorderStroke(1.dp, OpsColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = OpsColors.PanelLow, contentColor = OpsColors.Text),
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = OpsColors.Text,
    unfocusedTextColor = OpsColors.Text,
    focusedContainerColor = OpsColors.PanelLow,
    unfocusedContainerColor = OpsColors.PanelLow,
    focusedBorderColor = OpsColors.Cyan,
    unfocusedBorderColor = OpsColors.OutlineSoft,
    focusedLabelColor = OpsColors.Cyan,
    unfocusedLabelColor = OpsColors.Muted,
    focusedLeadingIconColor = OpsColors.Cyan,
    unfocusedLeadingIconColor = OpsColors.Muted,
    cursorColor = OpsColors.Cyan,
)

@Preview(showSystemUi = true)
@Composable
private fun RoleScreenPreview() {
    MonitorTheme { RoleScreen(onLogin = {}) }
}

@Composable
fun FigmaInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    trailing: ImageVector? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Color.White, fontSize = 13.sp, lineHeight = 16.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = AegizColors.Text),
            shape = RoundedCornerShape(6.dp),
            trailingIcon = trailing?.let { icon ->
                { Icon(icon, contentDescription = null, tint = AegizColors.Text) }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AegizColors.Text,
                unfocusedTextColor = AegizColors.Text,
                focusedContainerColor = AegizColors.Surface,
                unfocusedContainerColor = AegizColors.Surface,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = AegizColors.Red,
            ),
        )
    }
}

fun generateMissionCode(): String =
    kotlin.random.Random.nextInt(100000, 999999).toString()