# 🗺️ Roadmap Eksekusi Frontend Aegiz
## Besok tinggal ketik, semua nama fungsi/variabel sudah ada di sini

---

## ✅ TAHAP 1 — Setup Project

```
[ ] Buat project Android baru → Empty Activity (Jetpack Compose)
[ ] Package name: com.aegiz
[ ] Min SDK: 26 | Compile/Target SDK: 35
[ ] Tambah google-services.json ke folder app/
```

### `gradle/libs.versions.toml` — versi library wajib
```toml
[versions]
agp = "9.2.1"
kotlin = "2.2.10"
coreKtx = "1.13.1"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.10.01"
okhttp = "4.12.0"
osmdroid = "6.1.20"
googleServices = "4.4.4"
```

### `app/build.gradle.kts` — dependencies wajib
```kotlin
implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
implementation("com.google.firebase:firebase-auth")
implementation("com.google.firebase:firebase-database")
implementation(libs.okhttp)
implementation(libs.osmdroid)
implementation("io.coil-kt:coil-compose:2.7.0")
implementation(libs.androidx.material.icons.extended)
testImplementation("junit:junit:4.13.2")
testImplementation("org.json:json:20240303")
```

### `AndroidManifest.xml` — permission wajib
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

### Resource wajib di `res/`
```
res/font/plus_jakarta_sans_regular.ttf
res/font/plus_jakarta_sans_semibold.ttf
res/font/plus_jakarta_sans_bold.ttf
res/drawable/splash_img         ← gambar splash full-screen
res/drawable/avatar_hasan       ← foto avatar default anggota
```

---

## ✅ TAHAP 2 — `model/AppModels.kt`
**Buat semua ini dalam 1 file.**

### Enum & Config
```kotlin
enum class AppRole { RESCUER, PENANGGUNG_JAWAB }

enum class BleStage { IDLE, BLUETOOTH_OFF, NO_PERMISSION, SCANNING, NOT_FOUND, CONNECTING, CONNECTED, DISCONNECTED, ERROR }

enum class SosDeliveryState { NONE, SENDING, SENT, QUEUED, FAILED }

enum class RiskStatus(val label: String) {
    AMAN("Aman"), WASPADA("Waspada"), BAHAYA("Bahaya"), DARURAT("Darurat")
}
```

### Data Classes
```kotlin
data class AppUser(val uid: String, val fullname: String, val email: String, val role: String, val createdAt: Long)

data class DemoAccount(val username: String, val password: String, val role: AppRole, val userId: String, val displayName: String)

data class Esp32Telemetry(
    val deviceId: String?, val measuredAtEpoch: Long?,
    val gpsValid: Boolean = false, val lat: Double?, val lng: Double?, val charsProcessed: Long?,
    val loraOk: Boolean?, val wifiOk: Boolean?,
    val sosActive: Boolean = false, val sosSender: String?, val sosDeviceId: String?,
    val sosLat: Double?, val sosLon: Double?, val sosSource: String?,
    val sosPacketTimestamp: Long?, val sosStartMs: Long?
)

data class BleUiState(
    val stage: BleStage = BleStage.IDLE,
    val telemetry: Esp32Telemetry? = null,
    val message: String? = null,
    val internetAvailable: Boolean? = null,
    val phoneLat: Double? = null,
    val phoneLon: Double? = null,
    val sosDeliveryState: SosDeliveryState = SosDeliveryState.NONE,
    val pendingSosCount: Int = 0
)

data class MissionMeta(
    val title: String, val description: String, val category: String,
    val code: String, val status: String, val createdBy: String,
    val createdByName: String, val createdAt: Long, val startedAt: Long,
    val finishedAt: Long, val lat: Double?, val lon: Double?
)

data class MissionMember(
    val rescuerId: String, val name: String, val status: String,
    val riskScore: Int?, val riskStatus: String?,
    val lat: Double?, val lon: Double?, val updatedAt: Long?
)

data class SosEvent(
    val eventId: String?, val missionId: String, val rescuerId: String,
    val rescuerName: String, val deviceId: String, val status: String,
    val source: String, val lat: Double?, val lon: Double?,
    val createdAt: Long, val sosPacketTimestamp: Long?
)

data class GarminHealth(
    val rescuerId: String, val heartRate: Int?, val spo2: Int?,
    val stress: Int?, val bodyBattery: Int?, val respiration: Int?,
    val battery: Int?, val updatedAt: Long?, val source: String?
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
    val missionError: String? = null
)

data class RiskAssessment(val score: Int, val status: RiskStatus, val reason: String)
```

### Object Konstanta
```kotlin
object DemoConfig {
    const val MISSION_ID = "DEMO-01"
    const val MISSION_NAME = "Misi 01"
    const val RESPONSIBLE_ID = "PJ001"
    const val RESPONSIBLE_NAME = "Penanggung Jawab 01"
    const val RESCUER_ID = "R001"
    const val RESCUER_NAME = "Rescuer 01"
    const val DEVICE_ID = "DEV001"
    val accounts = listOf(
        DemoAccount("penanggungjawab", "demo123", AppRole.PENANGGUNG_JAWAB, "PJ001", "Penanggung Jawab 01"),
        DemoAccount("rescuer01", "demo123", AppRole.RESCUER, "R001", "Rescuer 01")
    )
}

object BleConfig {
    val RESCUER_DEVICE_NAMES = listOf("rescuer 01", "Rescuer01")
    val RECEIVER_DEVICE_NAMES = listOf("penanggung jawab 01", "Penanggungjawab01")
    val DEVICE_NAMES = RESCUER_DEVICE_NAMES + RECEIVER_DEVICE_NAMES
    val NUS_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val NUS_TX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    val NUS_RX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    const val SCAN_TIMEOUT_MS = 12_000L
}
```

---

## ✅ TAHAP 3 — `session/AppSession.kt`

### StateFlow yang dideklarasikan
```kotlin
object AppSession {
    // State
    private val _uid = MutableStateFlow<String?>(null)
    val uid: StateFlow<String?> = _uid.asStateFlow()

    private val _fullname = MutableStateFlow<String?>(null)
    val fullname: StateFlow<String?> = _fullname.asStateFlow()

    private val _email = MutableStateFlow<String?>(null)
    val email: StateFlow<String?> = _email.asStateFlow()

    private val _role = MutableStateFlow<AppRole?>(null)
    val role: StateFlow<AppRole?> = _role.asStateFlow()

    private val _activeMissionCode = MutableStateFlow<String?>(null)
    val activeMissionCode: StateFlow<String?> = _activeMissionCode.asStateFlow()

    private val _profilePhotoUri = MutableStateFlow<String?>(null)
    val profilePhotoUri: StateFlow<String?> = _profilePhotoUri.asStateFlow()
```

### Fungsi-fungsi wajib
```kotlin
    fun initialize(context: Context)
    fun currentRescuerId(): String   // uid ?: DemoConfig.RESCUER_ID
    fun currentRescuerName(): String // fullname ?: DemoConfig.RESCUER_NAME
    fun currentResponsibleId(): String
    fun currentResponsibleName(): String
    fun setUser(uid: String, fullname: String, email: String, role: AppRole)
    fun setRole(role: AppRole)
    fun clearRole()
    fun setActiveMission(code: String)   // normalize: uppercase, hapus karakter aneh
    fun clearActiveMission()
    fun setProfilePhoto(filePath: String)
    fun clearProfilePhoto()
    fun clearSession()           // signOut Firebase + hapus SharedPreferences
    fun context(): Context

    private const val PREFS = "aegiz_session"
    private const val KEY_ROLE = "app_role"
    private const val KEY_ACTIVE_MISSION = "active_mission_code"
    private const val KEY_UID = "auth_uid"
    private const val KEY_FULLNAME = "auth_fullname"
    private const val KEY_EMAIL = "auth_email"
    private const val KEY_PROFILE_PHOTO = "profile_photo_uri"
```

---

## ✅ TAHAP 4 — `logic/RiskLogic.kt`

### Konstanta bobot
```kotlin
private const val AMAN_THRESHOLD = 40
private const val WASPADA_THRESHOLD = 70
private const val WEIGHT_HEART_RATE = 0.36
private const val WEIGHT_BODY_BATTERY = 0.29
private const val WEIGHT_RESPIRATION = 0.21
private const val WEIGHT_STRESS = 0.14
private const val LOW_BODY_BATTERY_THRESHOLD = 20
private const val VERY_LOW_BODY_BATTERY_THRESHOLD = 10
private const val LOW_BODY_BATTERY_RISK_FLOOR = 40       // = AMAN_THRESHOLD
private const val VERY_LOW_BODY_BATTERY_RISK_FLOOR = 55
```

### Fungsi-fungsi
```kotlin
fun evaluateRisk(health: GarminHealth?, panicActive: Boolean = false): RiskAssessment
    // jika panicActive → langsung return RiskAssessment(100, DARURAT, "SOS aktif")
    // jika health null → gunakan default health (HR=78, BB=85, stress=22, resp=16)
    // hitung weightedScore → applyRiskFloors → tentukan status

private fun weightedScore(health: GarminHealth): Int
    // = (scoreHR * 0.36) + (scoreBB * 0.29) + (scoreResp * 0.21) + (scoreStress * 0.14)

private fun applyRiskFloors(health: GarminHealth, score: Int): Int
    // BB ≤ 10 → floor 55 | BB ≤ 20 → floor 40

private fun criticalReason(health: GarminHealth): String?
    // HR < 40 atau > 190 | Napas < 8 atau > 35 → return alasan, else null

private fun vitalScoreHeartRate(value: Int?): Int
    // 60-100: 0 | 101-140: 25 | 141-170: 50 | 40-59 atau 171-190: 75 | else: 100

private fun vitalScoreBodyBattery(value: Int?): Int
    // >80: 0 | 61-80: 25 | 41-60: 50 | 21-40: 75 | else: 100

private fun vitalScoreRespiration(value: Int?): Int
    // 12-20: 0 | 21-24: 25 | 25-29: 50 | 8-11 atau 30-34: 75 | else: 100

private fun vitalScoreStress(value: Int?): Int
    // ≤25: 0 | ≤50: 25 | ≤75: 50 | ≤90: 75 | else: 100
```

---

## ✅ TAHAP 5 — `logic/FirebaseLogic.kt`

### Config & Client
```kotlin
object FirebaseConfig {
    const val DATABASE_URL = "https://aegiz-ede38-default-rtdb.asia-southeast1.firebasedatabase.app/"
    const val AUTH_TOKEN = ""
    const val USERS_ROOT_PATH = "/users"
    const val SOS_PATH = "/devices/Rescuer01/sos/latest"
    const val GARMIN_ROOT_PATH = "/garmin_health"

    fun garminHealthPath(rescuerId: String): String   // /garmin_health/{id}/latest
    fun sosEventsPath(code: String): String           // /sos_events/{code}
    fun sosLatestPath(code: String): String           // /sos_latest/{code}
    fun sosEventPath(code: String, eventId: String): String
    fun missionMetaPath(code: String): String         // /missions/{code}/meta
    fun missionMembersPath(code: String): String      // /missions/{code}/members
    fun memberPath(code: String, rescuerId: String): String
    fun isConfigured(): Boolean
}

class FirebaseRestClient(baseUrl, authToken) {
    suspend fun get(path: String): String       // GET → path.json
    suspend fun put(path: String, jsonBody: String): String
    suspend fun patch(path: String, jsonBody: String): String
}
```

### JSON Extension Functions (parser JSON → data class)
```kotlin
fun JSONObject.toMissionMeta(fallbackCode: String): MissionMeta
fun JSONObject.toMissionMember(rescuerId: String): MissionMember
fun JSONObject.toGarminHealth(fallbackRescuerId: String): GarminHealth
fun JSONObject.toSosEvent(eventId: String): SosEvent
fun JSONObject.toAppUser(): AppUser

// Key Firebase ↔ Kotlin field mapping:
// "title", "description", "category", "code", "status", "created_by", "created_by_name"
// "created_at", "started_at", "finished_at", "lat", "lon"/"lng"
// "rescuer_id", "name", "risk_score", "risk_status", "updated_at"
// "heart_rate"/"hr", "spo2", "stress", "body_battery", "respiration"
// "mission_id", "rescuer_name", "device_id", "source", "created_at"/"ts"
```

### Data class → JSON (serializer)
```kotlin
fun MissionMeta.toJson(): JSONObject
fun MissionMember.toJson(): JSONObject
fun SosEvent.toJson(): String
fun AppUser.toJson(): JSONObject
```

### Helper null-safe JSON
```kotlin
fun JSONObject.optStringOrNull(key: String): String?
fun JSONObject.optIntOrNull(key: String): Int?
fun JSONObject.optLongOrNull(key: String): Long?
fun JSONObject.optDoubleOrNull(key: String): Double?
```

### Fungsi Bisnis Firebase (suspend)
```kotlin
fun login(username: String, password: String): DemoAccount?   // cek DemoConfig.accounts

suspend fun loginWithEmailPassword(email: String, pass: String): FirebaseUser
suspend fun registerWithEmailPassword(email: String, pass: String): FirebaseUser
suspend fun saveUserToFirebase(client: FirebaseRestClient, user: AppUser)
suspend fun fetchUserFromFirebase(client: FirebaseRestClient, uid: String): AppUser?

suspend fun createMission(client, code, title, description, category, createdBy, createdByName, lat?, lon?): MissionMeta
suspend fun joinMission(client, code, rescuerId, rescuerName): MissionMeta
suspend fun getMissionMeta(client, code): MissionMeta?
suspend fun finishMission(client, code)   // PUT status="finished" + finishedAt=now

suspend fun reportMember(client, code, health?, panicActive, lat?, lon?, rescuerId, rescuerName): RiskAssessment
suspend fun markMemberSos(client, code, rescuerId, rescuerName, lat?, lon?)
suspend fun resolveMember(client, code, rescuerId)   // status → AMAN + patch SOS events → RESOLVED

suspend fun pollMembers(client, code): List<MissionMember>   // sort by riskPriority
suspend fun pollSosEvents(client, code): List<SosEvent>      // sortedByDescending createdAt, take 20
suspend fun pollGarminHealth(client, rescuerId): GarminHealth?
suspend fun pollAllMissions(client, filterByCreator?, limit=20): List<MissionMeta>
suspend fun sendSosEventToFirebase(client, event): Boolean   // tulis ke 3 path

private suspend fun writeMember(client, code, member)
private fun String.riskPriority(): Int  // DARURAT=4, BAHAYA=3, WASPADA=2, else=1
```

---

## ✅ TAHAP 6 — `logic/BleLogic.kt`

### Class BleManager
```kotlin
class BleManager(application: Application) {
    val state: StateFlow<BleUiState>   // expose ke ViewModel

    fun startScan()             // scan BLE untuk Rescuer01 / Penanggungjawab01, timeout 12 detik
    fun disconnect()
    fun onPermissionDenied()
    fun dismissSos()            // clear SOS telemetry
    fun startLocationTracking() // GPS HP via LocationManager

    // Internal:
    private fun parseBleLine(line: String): Esp32Telemetry
    // Format 1: "SOS|R001|DEV001|-7.12|110.65|HP|12345"
    // Format 2: "GPS:NOFIX sat=0 | LoRa:OK | WiFi:OFF | SOS:ACTIVE sender=R001 device=DEV001 ..."
}

class SosOfflineQueue(application: Application) {
    fun enqueue(event: SosEvent)
    fun all(): List<SosEvent>
    fun remove(eventId: String?)
    fun count(): Int
}

fun SosEvent.stableQueueEventId(): String   // buat ID stabil dari rescuerId + createdAt
```

---

## ✅ TAHAP 7 — `ui/AppViewModels.kt`

### BleViewModel
```kotlin
class BleViewModel(application: Application) : AndroidViewModel(application) {
    val manager = BleManager(application)
    val state: StateFlow<BleUiState> = manager.state

    fun startScan()
    fun disconnect()
    fun onPermissionDenied()
    fun dismissSos()
    fun startLocationUpdates()  // memanggil manager.startLocationTracking()
}
```

### DashboardViewModel
```kotlin
class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val client = FirebaseRestClient()
    private val offlineQueue = SosOfflineQueue(application)
    private val _state = MutableStateFlow(Esp32UiState(garmin = null))
    val state: StateFlow<Esp32UiState> = _state.asStateFlow()
    private var pollingStarted = false

    fun startPolling(missionCode: String? = null)   // loop delay 1500ms
    fun restoreActiveMission()
    fun joinMission(code: String)
    fun createMission(code: String, title: String, desc: String, cat: String, lat: Double?, lon: Double?)
    fun finishMission()
    fun forwardBleSos(event: SosEvent)   // BLE relay → Firebase
    fun triggerSos(event: SosEvent)      // tombol SOS app → Firebase
    fun resolveMember(rescuerId: String)

    private suspend fun pollOnce()
    // urutan: drainOfflineQueue → garmin → history → (jika code aktif) meta + members + events
}
```

---

## ✅ TAHAP 8 — `ui/Theme.kt`

### OpsColors (dark, dipakai RoleScreen/Preview lama)
```kotlin
object OpsColors {
    val Background = Color(0xFF0B0F10)
    val Panel = Color(0xFF202425)
    val PanelLow = Color(0xFF111516)
    val Outline = Color(0xFF313A3C)
    val OutlineSoft = Color(0xFF1B2426)
    val Text = Color(0xFFE9F2F2)
    val Muted = Color(0xFF8C999B)
    val Cyan = Color(0xFF00CED1)
    val CyanDim = Color(0xFF0B5759)
    val RescueRed = Color(0xFFD32F2F)
    val RescueRedDark = Color(0xFF7E0813)
    val RescueRedSoft = Color(0xFFFFB0AD)
    val Amber = Color(0xFFFFC107)
}
```

### Font global (dideklarasikan di CommonUi.kt)
```kotlin
internal val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
)
```

### AegizColors (light, UTAMA dipakai di seluruh UI)
```kotlin
internal object AegizColors {
    val Background = Color(0xFFEFF2F6)   // abu muda
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
```

### Helper warna risk
```kotlin
fun riskColor(status: RiskStatus): Color      // AMAN→Green, WASPADA→Amber, BAHAYA→Orange, DARURAT→Red
fun riskTextColor(status: String): Color
fun String.isAlertStatus(): Boolean            // true jika DARURAT atau BAHAYA
```

---

## ✅ TAHAP 9 — `ui/CommonUi.kt` (file terbesar, 1647 baris)

### Helper format
```kotlin
fun formatMemberLocation(member: MissionMember): String?   // "%.4f, %.4f"
fun formatEventLocation(event: SosEvent): String?
```

### Layout Shell & Navigation
```kotlin
@Composable fun AegizShell(selected: Int, onSelected: (Int) -> Unit, content: @Composable (PaddingValues) -> Unit)
// 4 tab icons: Home, History, Input (Join), Person

@Composable fun AegizScrollContent(padding: PaddingValues, top: Dp, content: @Composable ColumnScope.() -> Unit)
// padding horizontal 20dp, statusBarsPadding, verticalScroll
```

### Komponen Header & Judul
```kotlin
@Composable fun DashboardHeader(greeting: String, subtitle: String)
// Row: teks kiri + notif bell + dot merah di kanan

@Composable fun CenterTitle(text: String)     // teks tengah 21sp SemiBold
@Composable fun SectionTitle(text: String)    // teks kiri 20sp SemiBold
@Composable fun SectionHeaderWithAction(title: String, action: String = "Lihat Semua")
```

### Komponen Alert
```kotlin
@Composable fun AegizAlertStrip(title: String, subtitle: String)
// Card DangerSoft + Warning icon + teks putih
```

### Komponen Device Status
```kotlin
@Composable fun AegizDeviceStatusCard(title: String, state: BleUiState, receiverMode: Boolean)
// Bluetooth icon + badge status + row GPS/LoRa/SOS

@Composable fun GarminDeviceStatusCard(health: GarminHealth?)
// MonitorHeart icon + badge + row HR/SpO2/BB

private fun AegizStatusLine(label: String, value: String, color: Color)
// Row label kiri muted + value kanan bold berwarna
```

### Komponen Metrik Vital
```kotlin
@Composable fun HealthMetricCard(label: String, value: String, status: String, modifier: Modifier, unit: String?, large: Boolean)
// Card 117dp: label atas, nilai merah besar, status muted bawah

@Composable fun ReadinessScoreCard(assessment: RiskAssessment)
// Card 117dp warna sesuai risk: skor 64sp di kanan, reason di kiri
```

### Komponen Misi
```kotlin
@Composable fun MissionSummaryCard(mission: MissionMeta?, members: List<MissionMember>, onClick: () -> Unit)
// Box 162dp: SirenArt dekorasi + Card merah 117dp + gradient + tombol "Lihat Selengkapnya"

@Composable fun EmptyMissionPrompt(onClick: () -> Unit)
// Card putih: ikon Input + teks "Belum Ada Misi" + ChevronRight

@Composable fun MissionDetailCard(mission, totalMembers, actionLabel, actionColor, onAction, stats: List<Pair<String,String>>)
// Card merah gradient: judul/deskripsi/kategori/kode + grid stat cards + tombol aksi

private fun MissionStatCard(label: String, value: String, modifier: Modifier)
// Surface putih 87dp: label atas + nilai merah besar

@Composable fun MissionFeedback(message: String?, error: String?, compact: Boolean = false)
// Teks hijau (sukses) atau merah (error)

fun generateMissionCode(): String   // generate kode misi random 6 karakter
```

### Komponen Roster Member
```kotlin
@Composable fun MemberRow(member: MissionMember, onResolved: (() -> Unit)? = null)
// ALERT state: Surface DangerSoft + avatar + nama + risk score + tombol "Tertolong" hijau
// NORMAL state: Surface putih + avatar + nama + status berwarna + ChevronRight

@Composable fun EmptyRosterCard()
// Surface 64dp: Person icon + "Belum ada rescuer"
```

### Komponen Peta
```kotlin
@Composable fun Esp32Map(lat: Double, lng: Double, modifier: Modifier = Modifier)
// AndroidView wrapping MapView osmdroid
// setTileSource(TileSourceFactory.MAPNIK)  ← ini yang panggil OpenStreetMap
// setMultiTouchControls(true), setUseDataConnection(true), setZoom(17.0)
// Marker dengan GeoPoint(lat, lng)

@Composable fun LiveSosMapCard(event: SosEvent, member: MissionMember?)
// Card: Esp32Map 335dp + row info member/lokasi
```

### Komponen Dialog SOS
```kotlin
@Composable fun EmergencyPopupDialog(event: SosEvent, member: MissionMember?, onDismiss: () -> Unit, onOpenMap: () -> Unit)
// Dialog SOS merah: info rescuer + koordinat + tombol "Buka Peta" dan "Mengerti"
```

### Komponen Profil
```kotlin
@Composable fun Avatar(size: Dp = 40.dp)
// Lingkaran gradien gelap + ikon Person putih

@Composable fun ProfilePhotoSection(size: Dp = 60.dp)
// AsyncImage (Coil) jika ada foto, Avatar jika tidak
// Badge kamera merah di pojok → launcher galeri ActivityResultContracts.GetContent()
// Copy ke filesDir/profiles/{uid}.jpg → AppSession.setProfilePhoto()
// Tombol "Hapus Foto" TextButton merah

@Composable fun ProfileBody(roleLabel, primaryStatus, secondaryStatus, state, health?, targetDeviceLabel, onScan, onDisconnect, onEnableBluetooth, onSos?, onChangeRole, padding)
// Card profil: foto + nama + badge peran + badge Online/Offline + email
// AegizDeviceStatusCard + GarminDeviceStatusCard (jika rescuer)
// Tombol: Scan, Nyalakan BT, Putuskan, SOS (opsional), GANTI PERAN
```

### Komponen Riwayat
```kotlin
@Composable fun HistoryBody(padding: PaddingValues, history: List<MissionMeta> = emptyList())
// State: selectedMission ← null = daftar | non-null = detail halaman
// → ActivityRow list, klik → MissionDetailPage

@Composable fun ActivityRow(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: (() -> Unit)? = null)
// Card: ikon berwarna + judul + subtitle + (ChevronRight jika clickable)

private fun MissionDetailPage(mission: MissionMeta, padding: PaddingValues, onBack: () -> Unit)
// Halaman detail: tombol back + info misi lengkap (started/finished timestamp + peta jika ada koordinat)
```

### Komponen Input
```kotlin
@Composable fun JoinMissionCard(message: String?, error: String?, onJoinMission: (String) -> Unit)
// Card: 6 kotak input kode + tombol Gabung + feedback

@Composable fun MissionCreateCard(message, error, phoneLat?, phoneLon?, onCreateMission)
// Card merah: input Judul, Deskripsi, Dropdown Kategori, kode auto-generate + Generate Ulang
// kategoriOptions = ["Banjir", "Tanah Longsor", "Gempa Bumi", "Kebakaran Hutan", ...]

@Composable fun FigmaInput(label: String, value: String, onValueChange: (String) -> Unit)
// OutlinedTextField white background di atas Card merah

@Composable fun SirenArt(modifier: Modifier, width: Dp = 118.dp)
// Dekorasi ilustrasi lampu siren (Box bertumpuk dengan gradient)
```

### Notifikasi
```kotlin
object SosNotifier {
    private const val CHANNEL_ID = "sos_alerts"
    fun ensureChannel(context: Context)
    fun notifySos(context: Context, eventId: String, rescuerName: String, lat: Double?, lon: Double?)
    // NotificationCompat.PRIORITY_HIGH + CATEGORY_ALARM + pendingIntent ke MainActivity
}
```

---

## ✅ TAHAP 10 — `ui/AuthUi.kt`

### Fungsi
```kotlin
@Composable fun AuthScreen(onLogin: (AppRole) -> Unit, onLoginSuccess: () -> Unit = {})
// showSplash = true → delay(2500) → showSplash = false → LoginScreen

private fun SplashScreen()
// Box fillMaxSize background AegizColors.Bkground + Image(R.drawable.splash_img, ContentScale.Crop)

private fun OnboardScreen(onStart: () -> Unit)   // sudah di-skip tapi masih ada kodenya

private fun LoginScreen(onLogin, onLoginSuccess, onBack)
// Background: splash_img grayscale + overlay hitam 65%
// Header: Security icon + "Aegiz"
// Card merah gradient: form login/register
//   - isRegister state → toggle form fullname + role selector
//   - Demo quick access: DemoRoleButton "Rescuer (Tim)" dan "Posko (PJ)"

private fun OnboardFeatureCard(icon: ImageVector, title: String, subtitle: String)
private fun DemoRoleButton(label: String, modifier: Modifier, onClick: () -> Unit)
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(...)
// white container, transparent border, red cursor
```

### State variabel di LoginScreen
```kotlin
var isRegister by rememberSaveable { mutableStateOf(false) }
var username by rememberSaveable { mutableStateOf("") }
var fullname by rememberSaveable { mutableStateOf("") }
var password by rememberSaveable { mutableStateOf("") }
var selectedRole by rememberSaveable { mutableStateOf(AppRole.RESCUER) }
var error by rememberSaveable { mutableStateOf<String?>(null) }
var loading by rememberSaveable { mutableStateOf(false) }
val scope = rememberCoroutineScope()
val client = remember { FirebaseRestClient() }
```

---

## ✅ TAHAP 11 — `ui/RescuerUi.kt`

### Entry point
```kotlin
@Composable fun AegizRescuerApp(state: BleUiState, firebaseState: Esp32UiState, onScan, onDisconnect, onEnableBluetooth, onSos, onJoinMission, onChangeRole)
// var tab by rememberSaveable { mutableIntStateOf(0) }  → 0=Dashboard, 1=Riwayat, 2=Misi, 3=Profil
// showSosSentDialog: AlertDialog konfirmasi setelah tombol SOS
```

### Fungsi internal
```kotlin
private fun RescuerDashboardBody(health: GarminHealth?, state: BleUiState, padding, missionHistory, onViewAllHistory)
// DashboardHeader + ReadinessScoreCard + HealthMetricCard x5 + daftar riwayat 5 terakhir

private fun RescuerMissionBody(state, firebaseState, onJoinMission, onSos, padding)
// Jika belum join: JoinMissionCard
// Jika sudah join: MissionDetailCard + Esp32Map 320dp + koordinat + tombol buka Google Maps

// Helper status label:
private fun heartRateStatus(value: Int): String    // Normal/Meningkat/Tinggi/Waspada/Kritis
private fun bodyBatteryStatus(value: Int): String  // Optimal/Baik/Menurun/Rendah/Kritis
private fun stressStatus(value: Int): String       // Rendah/Sedang/Tinggi/Waspada/Kritis
private fun respirationStatus(value: Int): String  // Normal/Meningkat/Tinggi/Waspada/Kritis
private fun spo2Status(value: Int): String         // Normal/Waspada/Kritis
```

---

## ✅ TAHAP 12 — `ui/SupervisorUi.kt`

### Entry point
```kotlin
@Composable fun AegizSupervisorApp(firebaseState, bleState, onScan, onDisconnect, onEnableBluetooth, onClearSos, onCreateMission, onFinishMission, onResolveMember, onChangeRole)
// var tab by rememberSaveable { mutableIntStateOf(0) }
// var dismissedEventId by rememberSaveable { mutableStateOf<String?>(null) }
// EmergencyPopupDialog muncul otomatis jika ada SOS DARURAT yang belum di-dismiss
```

### Fungsi internal
```kotlin
private fun SupervisorDashboardBody(state, bleState, onOpenMission, onViewAllHistory, padding)
// DashboardHeader + MissionSummaryCard (atau EmptyMissionPrompt) + AegizAlertStrip (jika SOS)
// LiveSosMapCard (jika ada koordinat SOS) + riwayat misi 3 terakhir

private fun SupervisorMissionBody(state, bleState, phoneLat?, phoneLon?, onCreateMission, onFinishMission, onResolveMember, padding)
// Jika misi aktif: MissionDetailCard + MemberRow forEach
// Jika tidak ada misi: MissionCreateCard + EmptyRosterCard

@Composable fun MissionCreateCard(message, error, phoneLat?, phoneLon?, onCreateMission)
// State: title, description, category (dropdown), code (auto-generate)
// LaunchedEffect(phoneLat, phoneLon) → auto-isi koordinat HP ke missionLat/missionLon
// fun generateMissionCode() → kode 6 karakter random [A-Z0-9]
```

---

## ✅ TAHAP 13 — `MainActivity.kt`

### Permission arrays
```kotlin
private val requiredBlePermissions = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
        arrayOf(BLUETOOTH_SCAN, BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
    else -> arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
}
private val allStartupPermissions = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
        requiredBlePermissions + arrayOf(POST_NOTIFICATIONS)
    else -> requiredBlePermissions
}
```

### ViewModels
```kotlin
private val bleViewModel by viewModels<BleViewModel>()
private val dashboardViewModel by viewModels<DashboardViewModel>()
```

### Launchers
```kotlin
private val permissionLauncher = registerForActivityResult(RequestMultiplePermissions()) { ... }
private val enableBtLauncher = registerForActivityResult(StartActivityForResult()) { ... }
```

### onCreate logic
```kotlin
AppSession.initialize(this)
dashboardViewModel.startPolling(AppSession.activeMissionCode.value)

// setContent:
// if currentRole == null → AuthScreen
// if RESCUER → AegizRescuerApp
// if PENANGGUNG_JAWAB → AegizSupervisorApp

// LaunchedEffect(bleState.telemetry?.sosStartMs):
// → auto-forward BLE SOS ke dashboardViewModel.forwardBleSos()

// Lambda handleScan: cek permission → startScan()
// Lambda handleEnableBt: cek permission → launch enableBtIntent
```

---

## 📋 Checklist Urutan Pengerjaan Final

```
[ ] 1. Setup project + Gradle + resource (font, drawable, google-services.json)
[ ] 2. AndroidManifest.xml permissions
[ ] 3. AppModels.kt
[ ] 4. AppSession.kt
[ ] 5. RiskLogic.kt
[ ] 6. FirebaseLogic.kt
[ ] 7. BleLogic.kt
[ ] 8. AppViewModels.kt
[ ] 9. Theme.kt
[ ] 10. CommonUi.kt (mulai dari AegizShell, lalu komponen satu-satu)
[ ] 11. AuthUi.kt
[ ] 12. RescuerUi.kt
[ ] 13. SupervisorUi.kt
[ ] 14. MainActivity.kt
[ ] 15. Build: ./gradlew :app:assembleDebug
```

---

## 🔑 Firebase Data Contract (referensi cepat)

| Path | Isi |
|---|---|
| `/missions/{code}/meta` | MissionMeta JSON |
| `/missions/{code}/members/{rescuerId}` | MissionMember JSON |
| `/sos_events/{code}/{eventId}` | SosEvent JSON |
| `/sos_latest/{code}` | SosEvent JSON terbaru |
| `/devices/Rescuer01/sos/latest` | SOS status dari ESP32 |
| `/garmin_health/R001/latest` | GarminHealth JSON |
| `/users/{uid}` | AppUser JSON |
| `/missions` (GET semua) | Root semua misi |

Database URL: `https://aegiz-ede38-default-rtdb.asia-southeast1.firebasedatabase.app/`
