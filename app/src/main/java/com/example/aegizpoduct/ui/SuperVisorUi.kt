package com.example.aegizpoduct.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.aegizpoduct.R
import com.example.aegizpoduct.logic.FirebaseRestClient
import com.example.aegizpoduct.logic.pollGarminHealth
import com.example.aegizpoduct.model.*
import com.example.aegizpoduct.session.AppSession
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun AegizSupervisorApp(
    firebaseState: Esp32UiState,
    bleState: BleUiState,
    onScan: () -> Unit,
    onDisconnect: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onClearSos: () -> Unit,
    onCreateMission: (String, String, String, String, Double?, Double?) -> Unit,
    onFinishMission: () -> Unit,
    onResolveMember: (String) -> Unit,
    onChangeRole: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var dismissedEventId by rememberSaveable { mutableStateOf<String?>(null) }

    val alertEvent = firebaseState.sosEvents.firstOrNull { it.status.equals("DARURAT", ignoreCase = true) }
    val activeSosEvent = alertEvent ?: if (bleState.telemetry?.sosActive == true) {
        val liveSenderId = bleState.telemetry.sosSender ?: DemoConfig.RESCUER_ID
        SosEvent(
            rescuerId = liveSenderId,
            // sosSender dari telemetry itu ID, bukan nama -> cari nama asli dari roster misi dulu
            rescuerName = firebaseState.members.firstOrNull { it.rescuerId == liveSenderId }
                ?.name?.takeIf { it.isNotBlank() } ?: liveSenderId,
            deviceId = bleState.telemetry.sosDeviceId ?: DemoConfig.DEVICE_ID,
            missionId = firebaseState.activeMissionCode ?: DemoConfig.MISSION_ID,
            status = "DARURAT",
            lat = bleState.telemetry.sosLat ?: bleState.telemetry.lat,
            lon = bleState.telemetry.sosLon ?: bleState.telemetry.lng,
            source = bleState.telemetry.sosSource ?: "LORA_RELAY",
            createdAt = bleState.telemetry.sosStartMs ?: System.currentTimeMillis()
        )
    } else null

    val currentEventId = activeSosEvent?.let { it.eventId ?: "${it.rescuerId}_${it.createdAt}" }
    if (activeSosEvent != null && currentEventId != dismissedEventId) {
        val distressedMember = firebaseState.members.firstOrNull { it.rescuerId == activeSosEvent.rescuerId }
        EmergencyPopupDialog(
            event = activeSosEvent,
            member = distressedMember,
            onDismiss = {
                dismissedEventId = currentEventId
                onClearSos()
            },
            onOpenMap = {
                dismissedEventId = currentEventId
                onClearSos()
                tab = 2
            }
        )
    }

    AegizShell(selected = tab, onSelected = { tab = it }) { padding ->
        when (tab) {
            0 -> SupervisorDashboardBody(
                state = firebaseState,
                bleState = bleState,
                onOpenMission = { tab = 2 },
                onViewAllHistory = { tab = 1 }, // navigasi ke tab riwayat
                padding = padding,
            )
            1 -> HistoryBody(padding, firebaseState.missionHistory)
            2 -> SupervisorMissionBody(
                state = firebaseState,
                bleState = bleState,
                phoneLat = bleState.phoneLat,
                phoneLon = bleState.phoneLon,
                onCreateMission = onCreateMission,
                onFinishMission = onFinishMission,
                onResolveMember = onResolveMember,
                padding = padding,
            )
            else -> ProfileBody(
                roleLabel = "Pengawas / Posko",
                primaryStatus = if (bleState.stage == BleStage.CONNECTED) "Receiver BLE tersambung" else "Receiver BLE belum tersambung",
                secondaryStatus = bleState.message ?: "Scan Penanggungjawab01 dari aplikasi untuk membaca relay LoRa.",
                state = bleState,
                health = null, // Pengawas/Posko tidak menggunakan Garmin watch
                targetDeviceLabel = "Penanggungjawab01",
                onScan = onScan,
                onDisconnect = onDisconnect,
                onEnableBluetooth = onEnableBluetooth,
                onSos = null,
                onChangeRole = onChangeRole,
                padding = padding,
            )
        }
    }
}

@Composable
private fun SupervisorDashboardBody(
    state: Esp32UiState,
    bleState: BleUiState,
    onOpenMission: () -> Unit,
    onViewAllHistory: () -> Unit, // fungsi untuk navigasi ke tab riwayat
    padding: PaddingValues,
) {
    val alertEvent = state.sosEvents.firstOrNull { it.status.equals("DARURAT", ignoreCase = true) }
    val missionActive = state.activeMissionCode != null

    AegizScrollContent(padding = padding, top = 38.dp) {
        DashboardHeader(greeting = "Halo, ${AppSession.currentResponsibleName()}", subtitle = "Jaga Koordinasi Dengan Baik.")
        SectionTitle("Misi Berlangsung")

        if (missionActive) {
            MissionSummaryCard(
                mission = state.missionMeta,
                members = state.members,
                onClick = onOpenMission,
            )
        } else {
            EmptyMissionPrompt(onClick = onOpenMission)
        }

        if (alertEvent != null) {
            AegizAlertStrip(
                title = "SOS aktif dari ${alertEvent.rescuerName.ifBlank { alertEvent.rescuerId }}",
                subtitle = formatEventLocation(alertEvent) ?: "Lokasi belum valid",
            )
        }

        if (alertEvent?.lat != null && alertEvent.lon != null) {
            LiveSosMapCard(
                event = alertEvent,
                member = state.members.firstOrNull { it.rescuerId == alertEvent.rescuerId },
            )
        }
        // Catatan: AegizDeviceStatusCard (BLE) DIPINDAHKAN ke halaman Profil
        // Dashboard tidak lagi menampilkan kartu koneksi bluetooth

        // Header riwayat misi dengan tombol "Lihat Semua" yang bisa diklik
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
            // Klik "Lihat Semua" akan berpindah ke tab riwayat
            Text(
                text = "Lihat Semua",
                color = AegizColors.Red,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
                modifier = androidx.compose.ui.Modifier.clickable { onViewAllHistory() }
            )
        }

        if (state.missionHistory.isEmpty()) {
            ActivityRow(
                icon = Icons.Default.CheckCircle,
                title = "Belum Ada Riwayat",
                subtitle = "Daftar misi yang selesai akan muncul di sini",
                color = Color(0xFF2E7D32),
            )
        } else {
            // Tampilkan maksimal 3 misi terakhir di dashboard
            state.missionHistory.take(3).forEach { mission ->
                val isActive = mission.status.equals("active", ignoreCase = true)
                ActivityRow(
                    icon = if (isActive) Icons.Default.Warning else Icons.Default.CheckCircle,
                    title = mission.title,
                    subtitle = "${mission.category} • ${if (isActive) "Aktif" else "Selesai"} (${mission.code})",
                    color = if (isActive) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                )
            }
        }
    }
}

@Composable
private fun SupervisorMissionBody(
    state: Esp32UiState,
    bleState: BleUiState,
    phoneLat: Double?,
    phoneLon: Double?,
    onCreateMission: (String, String, String, String, Double?, Double?) -> Unit,
    onFinishMission: () -> Unit,
    onResolveMember: (String) -> Unit,
    padding: PaddingValues,
) {
    AegizScrollContent(padding = padding, top = 54.dp) {
        CenterTitle("Misi")
        val missionActive = state.activeMissionCode != null
        if (missionActive) {
            SectionTitle("Misi Berlangsung")
            val members = state.members
            MissionDetailCard(
                mission = state.missionMeta,
                totalMembers = members.size,
                actionLabel = "Misi Selesai",
                actionColor = Color(0xFF37474F),
                onAction = onFinishMission,
                stats = listOf(
                    "Total Rescuer" to members.size.toString(),
                    "Koneksi ESP32" to if (members.any { it.updatedAt != null && it.updatedAt > 0 }) "Terkoneksi" else "Menunggu",
                    "Garmin Watch" to if (members.any { it.riskScore != null }) "Live Sync" else "Offline",
                    "Rescuer Aman" to members.count { it.status.equals("Aman", ignoreCase = true) }.toString(),
                ),
            )
            SectionHeaderWithAction(title = "Anggota Terhubung")
            if (members.isEmpty()) {
                EmptyRosterCard()
            } else {
                members.forEach { member ->
                    MemberRow(member = member, onResolved = { onResolveMember(member.rescuerId) })
                }
            }
        } else {
            SectionTitle("Belum Ada Misi Saat Ini")
            MissionCreateCard(
                message = state.missionMessage,
                error = state.missionError,
                phoneLat = phoneLat,
                phoneLon = phoneLon,
                onCreateMission = onCreateMission,
            )
            SectionHeaderWithAction(title = "Anggota Terhubung")
            EmptyRosterCard()
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MissionCreateCard(
    message: String?,
    error: String?,
    phoneLat: Double? = null,
    phoneLon: Double? = null,
    onCreateMission: (String, String, String, String, Double?, Double?) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("Longsor Magelang") }
    var description by rememberSaveable { mutableStateOf("Magelang Sector A") }
    var category by rememberSaveable { mutableStateOf("Rescue") }
    var code by rememberSaveable { mutableStateOf(generateMissionCode()) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    // Simpan lokasi HP yang diterima saat pertama kali composable terbuka
    var missionLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var missionLon by rememberSaveable { mutableStateOf<Double?>(null) }

    // Auto-isi lokasi dari HP saat pertama kali tersedia
    LaunchedEffect(phoneLat, phoneLon) {
        if (missionLat == null && phoneLat != null) missionLat = phoneLat
        if (missionLon == null && phoneLon != null) missionLon = phoneLon
    }

    // Daftar jenis bencana untuk dropdown kategori misi
    val kategoriOptions = listOf(
        "Banjir",
        "Tanah Longsor",
        "Gempa Bumi",
        "Kebakaran Hutan",
        "Tsunami",
        "Angin Puting Beliung",
        "Kekeringan",
        "Erupsi Gunung Berapi",
        "Kecelakaan Massal",
        "Rescue Umum",
    )
    // State untuk mengontrol buka/tutup dropdown
    var dropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AegizColors.Red),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(AegizColors.RedBright, AegizColors.RedDeep)))
                .padding(12.dp),
        ) {
            SirenArt(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 18.dp, y = (-16).dp),
                width = 112.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Buat Misi", color = Color.White, fontSize = 16.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold)
                FigmaInput(label = "Judul Misi", value = title, onValueChange = { title = it })
                FigmaInput(label = "Deskripsi", value = description, onValueChange = { description = it })

                // Dropdown untuk memilih kategori/jenis bencana
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Kategori Misi", color = Color.White, fontSize = 13.sp, lineHeight = 16.sp)
                    // ExposedDropdownMenuBox: komponen Material3 untuk dropdown pilihan
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }, // toggle buka/tutup
                    ) {
                        // Tampilan nilai yang dipilih (mirip text field tapi read-only)
                        OutlinedTextField(
                            value = category,
                            onValueChange = {}, // read-only, tidak menerima input langsung
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(), // penting: menandai ini sebagai anchor dropdown
                            readOnly = true,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AegizColors.Text,
                                unfocusedTextColor = AegizColors.Text,
                                focusedContainerColor = AegizColors.Surface,
                                unfocusedContainerColor = AegizColors.Surface,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                            ),
                            trailingIcon = {
                                // Ikon panah yang berputar saat dropdown terbuka
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = AegizColors.Text
                                )
                            },
                            shape = RoundedCornerShape(6.dp),
                        )
                        // Daftar pilihan dropdown yang muncul saat diklik
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }, // tutup jika klik di luar
                        ) {
                            kategoriOptions.forEach { opsi ->
                                // Setiap item kategori bencana
                                DropdownMenuItem(
                                    text = { Text(opsi, fontFamily = PlusJakartaSans) },
                                    onClick = {
                                        category = opsi          // simpan pilihan
                                        dropdownExpanded = false // tutup dropdown
                                    }
                                )
                            }
                        }
                    }
                }

                Text("Kode Misi", color = Color.White, fontSize = 13.sp, lineHeight = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    code.forEach { char ->
                        Surface(
                            modifier = Modifier.weight(1f).height(60.dp),
                            color = AegizColors.Surface,
                            shape = RoundedCornerShape(5.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(char.toString(), color = AegizColors.Text, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { code = generateMissionCode() }) {
                        Text("Generate Ulang", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    MissionFeedback(message = message, error = localError ?: error, compact = true)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = {
                            localError = null
                            if (title.isBlank()) {
                                localError = "Judul wajib diisi"
                            } else {
                                onCreateMission(code, title, description, category, missionLat, missionLon)
                            }
                        },
                        modifier = Modifier.width(154.dp).height(45.dp),
                        shape = RoundedCornerShape(7.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AegizColors.DarkCard, contentColor = Color.White),
                    ) {
                        Text("Mulai Misi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyPopupDialog(
    event: SosEvent,
    member: MissionMember?,
    onDismiss: () -> Unit,
    onOpenMap: () -> Unit,
) {
    var rescuerHealth by remember { mutableStateOf<GarminHealth?>(null) }
    val client = remember { FirebaseRestClient() }

    LaunchedEffect(event.rescuerId) {
        while (true) {
            val h = pollGarminHealth(client, event.rescuerId)
            if (h != null) {
                rescuerHealth = h
            }
            delay(2000L)
        }
    }

    val timeText = remember(event.createdAt) {
        if (event.createdAt > 0) {
            val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Jakarta")
            }
            "${sdf.format(java.util.Date(event.createdAt))} WIB"
        } else {
            "--:--:--"
        }
    }

    val locationText = remember(event.lat, event.lon) {
        if (event.lat != null && event.lon != null) {
            "%.4f, %.4f".format(java.util.Locale.US, event.lat, event.lon)
        } else {
            "Tidak Diketahui"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AegizColors.Red,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Anggota Darurat",
                        color = AegizColors.Red,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans
                    )
                }

                // Thin red divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AegizColors.Red)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Profile Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE58787))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.avatar_hasan),
                            contentDescription = "Avatar Rescuer",
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = event.rescuerName.ifBlank { "Hasan Hakim" },
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSans
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Darurat",
                                color = Color(0xFFB80000),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSans
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Info Grid: Lokasi Terakhir & Waktu Terakhir
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lokasi Terakhir :",
                            color = AegizColors.Muted,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = locationText,
                            color = AegizColors.Text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Waktu Terakhir :",
                            color = AegizColors.Muted,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = timeText,
                            color = AegizColors.Text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Info Grid: Condition Score & Heart Rate
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Risk Score :",
                            color = AegizColors.Muted,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = (member?.riskScore ?: 80).toString(),
                            color = AegizColors.Text,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Heart Rate",
                            color = AegizColors.Muted,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = (rescuerHealth?.heartRate ?: 150).toString(),
                            color = AegizColors.Red,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Respons Button
                Button(
                    onClick = onOpenMap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegizColors.Red,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Respons",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans
                    )
                }
            }
        }
    }
}