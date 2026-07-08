package com.example.aegizpoduct.logic

import androidx.privacysandbox.ads.adservices.adid.AdId
import com.example.aegizpoduct.Model.AppRole
import com.example.aegizpoduct.Model.AppUser
import com.example.aegizpoduct.Model.DemoConfig
import com.example.aegizpoduct.Model.DemoAccount
import com.example.aegizpoduct.Model.MissionMeta
import com.example.aegizpoduct.Model.GarminHealth
import com.example.aegizpoduct.Model.MissionMember
import com.example.aegizpoduct.Model.RiskStatus
import com.example.aegizpoduct.Model.RiskAssessment
import com.example.aegizpoduct.Model.SosEvent
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FirebaseConfig {
    const val DATABASE_URL = "https://aegiz-ede38-default-rtdb.asia-southeast1.firebasedatabase.app/"
    const val AUTH_TOKEN = ""
    const val USERS_ROOT_PATH ="/users"
    const val SOS_PATH ="devices/Rescuer01/sos/latest"
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
        trim().replace(Regex("""[.#$\[\]/]"""), "_")
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

fun JSONObject.toMissionMeta(fallbackCode: String= DemoConfig.MISSION_ID): MissionMeta =
    MissionMeta(
        title = optStringOrNull("title") ?: DemoConfig.MISSION_NAME,
        description = optStringOrNull("description").orEmpty(),
        category = optStringOrNull("category").orEmpty(),
        code = optStringOrNull("code")?: fallbackCode,
        status = optStringOrNull("status") ?: "active",
        createdBy = optStringOrNull("created_by")?: DemoConfig.RESPONSIBLE_ID,
        createdByName = optStringOrNull("created_by_name").orEmpty(),
        createdAt = optLongOrNull("created_at") ?: 0L,
        startedAt = optLongOrNull("started_at") ?: 0L,
        finishedAt = optLongOrNull("finished_at") ?: 0L,
        lat = optDoubleOrNull("lat"),
        lon = optDoubleOrNull("lon") ?: optDoubleOrNull("lng"),
    )

fun JSONObject.toMissionMember(rescuerId: String) : MissionMember =
    MissionMember(
        rescuerId = optStringOrNull("rescuerId") ?: rescuerId,
        name = optStringOrNull("name") ?: rescuerId,
        status = optStringOrNull("status") ?: RiskStatus.AMAN.label,
        riskScore = optIntOrNull("riskScore"),
        riskStatus = optStringOrNull("risk_status"),
        lat = optDoubleOrNull("lat"),
        lon = optDoubleOrNull("lon") ?: optDoubleOrNull("lng"),
        updatedAt = optLongOrNull("update_at"),
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
        .put("created_by_name", createdByName)
        .put("created_at", createdAt)
        .put("started_at", startedAt)
        .put("finished_at", finishedAt)
        .apply { if (lat != null) put("lat", lat) }
        .apply { if (lon != null) put("lon", lon) }

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


