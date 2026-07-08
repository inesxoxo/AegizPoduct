package com.example.aegizpoduct.logic

import androidx.privacysandbox.ads.adservices.adid.AdId
import com.example.aegizpoduct.Model.AppRole
import com.example.aegizpoduct.Model.AppUser
import com.example.aegizpoduct.Model.DemoConfig
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
        createdBy = optStringOrNull("createdBy")?: DemoConfig.RESPONSIBLE_ID,
        createdByName = optStringOrNull("createdByName").orEmpty(),
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
        riskStatus = optStringOrNull("riskStatus"),
        lat = optDoubleOrNull("lat"),
        lon = optDoubleOrNull("lon") ?: optDoubleOrNull("lng"),
        updatedAt = optLongOrNull("updateAt") ?: 0L,
    )

fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) optString(key) else null

fun JSONObject.optIntOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null

fun JSONObject.optLongOrNull(key: String): Long? =
    if (has(key) && !isNull(key)) optLong(key) else null

fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (has(key) && !isNull(key)) optDouble(key) else null