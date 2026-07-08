package com.example.aegizpoduct.logic

import com.example.aegizpoduct.model.AppRole
import com.example.aegizpoduct.model.AppUser
import com.example.aegizpoduct.model.DemoConfig
import com.example.aegizpoduct.model.DemoAccount
import com.example.aegizpoduct.model.MissionMeta
import com.example.aegizpoduct.model.GarminHealth
import com.example.aegizpoduct.model.MissionMember
import com.example.aegizpoduct.model.RiskStatus
import com.example.aegizpoduct.model.RiskAssessment
import com.example.aegizpoduct.model.SosEvent
import com.example.aegizpoduct.session.AppSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.code
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.text.category
import kotlin.toString

object FirebaseConfig {
    const val DATABASE_URL = "https://aegiz-ede38-default-rtdb.asia-southeast1.firebasedatabase.app/"
    const val AUTH_TOKEN = ""

    const val USERS_ROOT_PATH = "/users"
    const val SOS_PATH = "/devices/Rescuer01/sos/latest"
    const val GARMIN_ROOT_PATH = "/garmin_health"

    fun garminHealthPath(rescuerId: String): String = "$GARMIN_ROOT_PATH/${rescuerId.normalizedKey()}/latest"
    fun sosEventsPath(code: String): String = "/sos_events/${code.normalizedMissionCode()}"
    fun sosLatestPath(code: String): String = "/sos_latest/${code.normalizedMissionCode()}"
    fun sosEventPath(code: String, eventId: String): String = "${sosEventsPath(code)}/$eventId"
    fun missionMetaPath(code: String): String = "/missions/${code.normalizedMissionCode()}/meta"
    fun missionMembersPath(code: String): String = "/missions/${code.normalizedMissionCode()}/members"
    fun memberPath(code: String, rescuerId: String): String =
        "${missionMembersPath(code)}/${rescuerId.normalizedKey()}"

    fun isConfigured(): Boolean = DATABASE_URL.isNotBlank() && !DATABASE_URL.contains("ISI_PROJECT_ID")

    private fun String.normalizedMissionCode(): String =
        trim().uppercase().replace(Regex("""[^A-Z0-9_-]"""), "_")

    fun String.normalizedKey(): String =
        trim()
            .replace(Regex("""[.#$\[\]/ ]"""), "_")
}

class FirebaseRestClient(
    private val baseUrl: String = FirebaseConfig.DATABASE_URL,
    private val authToken: String = FirebaseConfig.AUTH_TOKEN,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun urlFor(path: String): String {
        val base = baseUrl.trimEnd('/')
        var url = "$base$path.json"
        if (authToken.isNotBlank()) url += "?auth=$authToken"
        return url
    }

    suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(urlFor(path)).get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            check(resp.isSuccessful) { "HTTP ${resp.code}: ${body.take(120)}" }
            body
        }
    }

    suspend fun put(path: String, jsonBody: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(urlFor(path)).put(jsonBody.toRequestBody(jsonMedia)).build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            check(resp.isSuccessful) { "HTTP ${resp.code}: ${body.take(120)}" }
            body
        }
    }

    suspend fun patch(path: String, jsonBody: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(urlFor(path)).patch(jsonBody.toRequestBody(jsonMedia)).build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            check(resp.isSuccessful) { "HTTP ${resp.code}: ${body.take(120)}" }
            body
        }
    }
}

fun JSONObject.toMissionMeta(fallbackCode: String = DemoConfig.MISSION_ID): MissionMeta =
    MissionMeta(
        title = optStringOrNull("title") ?: DemoConfig.MISSION_NAME,
        description = optStringOrNull("description").orEmpty(),
        category = optStringOrNull("category").orEmpty(),
        code = optStringOrNull("code") ?: fallbackCode,
        status = optStringOrNull("status") ?: "active",
        createdBy = optStringOrNull("created_by") ?: DemoConfig.RESPONSIBLE_ID,
        createdByName = optStringOrNull("created_by_name").orEmpty(), // nama pembuat misi
        createdAt = optLongOrNull("created_at") ?: 0L,
        startedAt = optLongOrNull("started_at") ?: 0L,   // jam mulai misi (epoch ms)
        finishedAt = optLongOrNull("finished_at") ?: 0L, // jam selesai misi (epoch ms)
        lat = optDoubleOrNull("lat"),                     // latitude lokasi misi
        lon = optDoubleOrNull("lon") ?: optDoubleOrNull("lng"), // longitude lokasi misi
    )

fun JSONObject.toMissionMember(defaultRescuerId: String): MissionMember =
    MissionMember(
        rescuerId =
            optStringOrNull("rescuer_id")
                ?: optStringOrNull("rescuerId")
                ?: defaultRescuerId,

        name =
            optStringOrNull("name")
                ?: defaultRescuerId,

        status =
            optStringOrNull("status")
                ?: RiskStatus.AMAN.label,

        riskScore =
            optIntOrNull("risk_score")
                ?: optIntOrNull("riskScore"),

        riskStatus =
            optStringOrNull("risk_status")
                ?: optStringOrNull("riskStatus"),

        lat =
            optDoubleOrNull("lat"),

        lon =
            optDoubleOrNull("lon")
                ?: optDoubleOrNull("lng"),

        updatedAt =
            optLongOrNull("updated_at")
                ?: optLongOrNull("updatedAt")
    )

fun JSONObject.toGarminHealth(fallbackRescuerId: String = DemoConfig.RESCUER_ID): GarminHealth =
    GarminHealth(
        rescuerId = optStringOrNull("rescuer_id") ?: fallbackRescuerId,
        heartRate = optIntOrNull("heart_rate") ?: optIntOrNull("hr"),
        spo2 = optIntOrNull("spo2") ?: optIntOrNull("spO2"),
        stress = optIntOrNull("stress"),
        bodyBattery = optIntOrNull("body_battery") ?: optIntOrNull("body_energy"),
        respiration = optIntOrNull("respiration") ?: optIntOrNull("respiration_rate"),
        battery = optIntOrNull("battery"),
        updatedAt = optLongOrNull("updated_at") ?: optLongOrNull("ts"),
        source = optStringOrNull("source"),
    )

fun JSONObject.toSosEvent(eventId: String): SosEvent =
    SosEvent(
        eventId = eventId,
        missionId = optStringOrNull("mission_id") ?: DemoConfig.MISSION_ID,
        rescuerId = optStringOrNull("rescuer_id") ?: optStringOrNull("sender") ?: DemoConfig.RESCUER_ID,
        rescuerName = optStringOrNull("rescuer_name") ?: DemoConfig.RESCUER_NAME,
        deviceId = optStringOrNull("device_id") ?: DemoConfig.DEVICE_ID,
        status = optStringOrNull("status") ?: "DARURAT",
        source = optStringOrNull("source") ?: "unknown",
        lat = optDoubleOrNull("lat"),
        lon = optDoubleOrNull("lon") ?: optDoubleOrNull("lng"),
        createdAt = optLongOrNull("created_at") ?: optLongOrNull("ts") ?: 0L,
        sosPacketTimestamp = optLongOrNull("sos_packet_ts") ?: optLongOrNull("relay_ts") ?: optLongOrNull("sosTs"),
    )

fun JSONObject.toAppUser(): AppUser =
    AppUser(
        uid = optStringOrNull("uid").orEmpty(),
        fullname = optStringOrNull("fullname").orEmpty(),
        email = optStringOrNull("email").orEmpty(),
        role = optStringOrNull("role").orEmpty(),
        createdAt = optLongOrNull("createdAt") ?: optLongOrNull("created_at") ?: 0L,
    )

fun MissionMeta.toJson(): JSONObject =
    JSONObject()
        .put("title", title)
        .put("description", description)
        .put("category", category)
        .put("code", code)
        .put("status", status)
        .put("created_by", createdBy)
        .put("created_by_name", createdByName)  // simpan nama pembuat ke Firebase
        .put("created_at", createdAt)
        .put("started_at", startedAt)   // simpan jam mulai ke Firebase
        .put("finished_at", finishedAt) // simpan jam selesai ke Firebase
        .apply { if (lat != null) put("lat", lat) }  // simpan latitude jika ada
        .apply { if (lon != null) put("lon", lon) }  // simpan longitude jika ada

fun MissionMember.toJson(): JSONObject =
    JSONObject()
        .put("rescuer_id", rescuerId)
        .put("name", name)
        .put("status", status)
        .put("risk_score", riskScore)
        .put("risk_status", riskStatus)
        .put("lat", lat)
        .put("lon", lon)
        .put("updated_at", updatedAt ?: System.currentTimeMillis() / 1000)

fun SosEvent.toJson(): String =
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
        .toString()

fun AppUser.toJson(): JSONObject =
    JSONObject()
        .put("uid", uid)
        .put("fullname", fullname)
        .put("email", email)
        .put("role", role)
        .put("createdAt", createdAt)

fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) optString(key) else null

fun JSONObject.optIntOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null

fun JSONObject.optLongOrNull(key: String): Long? =
    if (has(key) && !isNull(key)) optLong(key) else null

fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (has(key) && !isNull(key)) optDouble(key) else null

fun login(username: String, password: String): DemoAccount? =
    DemoConfig.accounts.firstOrNull {
        it.username.equals(username.trim(), ignoreCase = true) && it.password == password
    }

suspend fun saveUserToFirebase(client: FirebaseRestClient, user: AppUser) {
    require(user.uid.isNotBlank()) { "uid tidak boleh kosong" }
    client.put("${FirebaseConfig.USERS_ROOT_PATH}/${user.uid}", user.toJson().toString())
}

suspend fun fetchUserFromFirebase(client: FirebaseRestClient, uid: String): AppUser? = runCatching {
    val raw = client.get("${FirebaseConfig.USERS_ROOT_PATH}/$uid")
    if (raw.isBlank() || raw == "null") null else JSONObject(raw).toAppUser()
}.getOrNull()

suspend fun loginWithEmailPassword(email: String, pass: String): FirebaseUser =
    suspendCancellableCoroutine { cont ->
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { res ->
                val u = res.user
                if (u != null) cont.resume(u) else cont.resumeWithException(IllegalStateException("User tidak ditemukan"))
            }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

suspend fun registerWithEmailPassword(email: String, pass: String): FirebaseUser =
    suspendCancellableCoroutine { cont ->
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { res ->
                val u = res.user
                if (u != null) cont.resume(u) else cont.resumeWithException(IllegalStateException("Registrasi gagal"))
            }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

suspend fun createMission(
    client: FirebaseRestClient,
    code: String,
    title: String,
    description: String,
    category: String,
    createdBy: String = AppSession.currentResponsibleId(),
    createdByName: String = AppSession.currentResponsibleName(), // simpan nama supervisor
    lat: Double? = null,  // latitude lokasi HP supervisor saat misi dibuat
    lon: Double? = null,  // longitude lokasi HP supervisor saat misi dibuat
): MissionMeta {
    val now = System.currentTimeMillis() // catat waktu sekarang sebagai jam mulai
    val meta = MissionMeta(
        title = title.trim(),
        description = description.trim(),
        category = category.trim(),
        code = code.trim().uppercase(),
        status = "active",
        createdBy = createdBy,
        createdByName = createdByName,  // nama supervisor tersimpan ke Firebase
        createdAt = now / 1000,
        startedAt = now,  // simpan jam mulai misi (epoch millisecond)
        lat = lat,        // koordinat lokasi HP supervisor (latitude)
        lon = lon,        // koordinat lokasi HP supervisor (longitude)
    )
    client.put(FirebaseConfig.missionMetaPath(meta.code), meta.toJson().toString())
    return meta
}

suspend fun joinMission(
    client: FirebaseRestClient,
    code: String,
    rescuerId: String = AppSession.currentRescuerId(),
    rescuerName: String = AppSession.currentRescuerName(),
): MissionMeta {
    val missionCode = code.trim().uppercase()
    val meta = getMissionMeta(client, missionCode) ?: error("Kode misi $missionCode tidak ditemukan")
    if (!meta.status.equals("active", ignoreCase = true)) {
        error("Misi ${meta.code} sudah tidak aktif")
    }
    writeMember(
        client,
        missionCode,
        MissionMember(
            rescuerId = rescuerId,
            name = rescuerName,
            status = RiskStatus.AMAN.label,
            riskScore = 0,
            riskStatus = RiskStatus.AMAN.name,
            updatedAt = System.currentTimeMillis() / 1000,
        ),
    )
    return meta
}

suspend fun getMissionMeta(client: FirebaseRestClient, code: String): MissionMeta? {
    val raw = client.get(FirebaseConfig.missionMetaPath(code))
    if (raw.isBlank() || raw == "null") return null
    return JSONObject(raw).toMissionMeta(code)
}

suspend fun finishMission(client: FirebaseRestClient, code: String) {
    val meta = getMissionMeta(client, code) ?: return
    // Salin meta dengan status finished dan catat jam selesai sekarang
    val finishedMeta = meta.copy(
        status = "finished",
        finishedAt = System.currentTimeMillis(), // catat jam selesai misi (epoch ms)
    )
    client.put(FirebaseConfig.missionMetaPath(code), finishedMeta.toJson().toString())
}

suspend fun reportMember(
    client: FirebaseRestClient,
    code: String,
    health: GarminHealth?,
    panicActive: Boolean,
    lat: Double?,
    lon: Double?,
    rescuerId: String = AppSession.currentRescuerId(),
    rescuerName: String = AppSession.currentRescuerName(),
): RiskAssessment {
    val assessment = evaluateRisk(health, panicActive)
    val currentMember =
        pollMembers(client, code)
            .firstOrNull {
                it.rescuerId.equals(rescuerId, ignoreCase = true)
            }

    if (currentMember?.status == RiskStatus.DARURAT.label) {
        return RiskAssessment(
            score = 100,
            status = RiskStatus.DARURAT,
            reason = "SOS sedang aktif"

        )
    }
    writeMember(
        client,
        code,
        MissionMember(
            rescuerId = rescuerId,
            name = rescuerName,
            status = assessment.status.label,
            riskScore = assessment.score,
            riskStatus = assessment.status.name,
            lat = lat,
            lon = lon,
            updatedAt = System.currentTimeMillis() / 1000,
        ),
    )
    return assessment
}

suspend fun markMemberSos(
    client: FirebaseRestClient,
    code: String,
    rescuerId: String,
    rescuerName: String,
    lat: Double?,
    lon: Double?,
) {
    writeMember(
        client,
        code,
        MissionMember(
            rescuerId = rescuerId,
            name = rescuerName,
            status = RiskStatus.DARURAT.label,
            riskScore = 100,
            riskStatus = RiskStatus.DARURAT.name,
            lat = lat,
            lon = lon,
            updatedAt = System.currentTimeMillis() / 1000,
        ),
    )
}

suspend fun resolveMember(client: FirebaseRestClient, code: String, rescuerId: String) {
    // 1. Update status anggota di Firebase agar kembali normal (Aman/0)
    val patch = JSONObject()
        .put("status", RiskStatus.AMAN.label)
        .put("risk_score", 0)
        .put("risk_status", RiskStatus.AMAN.name)
    client.patch(FirebaseConfig.memberPath(code, rescuerId), patch.toString())

    // 2. Cari semua event SOS untuk rescuerId ini yang masih berstatus "DARURAT" dan tandai sebagai "RESOLVED"
    runCatching {
        val rawEvents = client.get(FirebaseConfig.sosEventsPath(code))
        if (rawEvents.isNotBlank() && rawEvents != "null") {
            val root = JSONObject(rawEvents)
            root.keys().forEach { key ->
                val obj = root.optJSONObject(key)
                if (obj != null) {
                    val rId = obj.optString("rescuer_id").takeIf { it.isNotBlank() } ?: obj.optString("sender")
                    val status = obj.optString("status")
                    if (rId == rescuerId && status.equals("DARURAT", ignoreCase = true)) {
                        // Patch status event SOS ini di Firebase agar menjadi RESOLVED
                        val eventPatch = JSONObject().put("status", "RESOLVED")
                        client.patch(FirebaseConfig.sosEventPath(code, key), eventPatch.toString())
                    }
                }
            }
        }
    }
}

suspend fun pollMembers(client: FirebaseRestClient, code: String): List<MissionMember> = runCatching {
    val raw = client.get(FirebaseConfig.missionMembersPath(code))
    if (raw.isBlank() || raw == "null") return@runCatching emptyList()
    val root = JSONObject(raw)
    root.keys().asSequence()
        .mapNotNull { key -> root.optJSONObject(key)?.toMissionMember(key) }
        .sortedWith(compareByDescending<MissionMember> { it.status.riskPriority() }.thenBy { it.name })
        .toList()
}.getOrDefault(emptyList())

private suspend fun writeMember(client: FirebaseRestClient, code: String, member: MissionMember) {
    client.put(FirebaseConfig.memberPath(code, member.rescuerId), member.toJson().toString())
}

private fun String.riskPriority(): Int = when {
    equals(RiskStatus.DARURAT.label, ignoreCase = true) -> 4
    equals(RiskStatus.BAHAYA.label, ignoreCase = true) -> 3
    equals(RiskStatus.WASPADA.label, ignoreCase = true) -> 2
    else -> 1
}

suspend fun pollSosEvents(client: FirebaseRestClient, code: String): List<SosEvent> = runCatching {
    val raw = client.get(FirebaseConfig.sosEventsPath(code))
    if (raw.isBlank() || raw == "null") return@runCatching emptyList()
    val root = JSONObject(raw)
    root.keys().asSequence()
        .mapNotNull { key -> root.optJSONObject(key)?.toSosEvent(key) }
        .sortedByDescending { it.createdAt }
        .take(20)
        .toList()
}.getOrDefault(emptyList())

suspend fun sendSosEventToFirebase(client: FirebaseRestClient, event: SosEvent): Boolean {
    if (!FirebaseConfig.isConfigured()) return false
    val eventId = event.stableQueueEventId()
    val json = event.copy(eventId = eventId).toJson()

    val eventWritten = runCatching {
        client.put(FirebaseConfig.sosEventPath(event.missionId, eventId), json)
    }.isSuccess
    if ( eventWritten){

        markMemberSos(
            client = client,
            code = event.missionId,
            rescuerId =  event.rescuerId,
            rescuerName = event.rescuerName,
            lat = event.lat,
            lon = event.lon
        )
    }

    runCatching { client.put(FirebaseConfig.sosLatestPath(event.missionId), json) }
    runCatching {
        client.put(
            FirebaseConfig.SOS_PATH,
            JSONObject()
                .put("active", true)
                .put("sender", event.rescuerId)
                .put("ts", event.createdAt)
                .put("source", event.source)
                .put("lat", event.lat)
                .put("lon", event.lon)
                .put("event_id", eventId)
                .toString(),
        )
    }
    return eventWritten
}

suspend fun pollGarminHealth(client: FirebaseRestClient, rescuerId: String = AppSession.currentRescuerId()): GarminHealth? = runCatching {
    val raw = client.get(FirebaseConfig.garminHealthPath(rescuerId))
    if (raw.isBlank() || raw == "null") return@runCatching null
    JSONObject(raw).toGarminHealth(rescuerId)
}.getOrNull()

suspend fun pollAllMissions(
    client: FirebaseRestClient,
    filterByCreator: String? = if (AppSession.role.value == AppRole.PENANGGUNG_JAWAB) AppSession.currentResponsibleId() else null,
    limit: Int = 20
): List<MissionMeta> = runCatching {
    val raw = client.get("/missions")
    if (raw.isBlank() || raw == "null") return@runCatching emptyList()
    val root = JSONObject(raw)
    root.keys().asSequence()
        .mapNotNull { code ->
            val obj = root.optJSONObject(code)?.optJSONObject("meta") ?: root.optJSONObject(code)
            obj?.toMissionMeta(code)
        }
        .filter {
            if (AppSession.role.value == AppRole.RESCUER) {
                // Untuk penyelamat (Rescuer), hanya tampilkan misi yang diikutinya (ada di members)
                val missionObj = root.optJSONObject(it.code)
                val membersObj = missionObj?.optJSONObject("members")
                val normalizedRescuerId = AppSession.currentRescuerId().replace(Regex("""[.#$\[\]/]"""), "_")
                membersObj != null && (membersObj.has(normalizedRescuerId) || membersObj.has(AppSession.currentRescuerId()))
            } else {
                filterByCreator == null || it.createdBy == filterByCreator
            }
        }
        .sortedByDescending { it.createdAt }
        .take(limit)
        .toList()
}.getOrDefault(emptyList())

