package com.example.aegizpoduct.session

import android.content.Context
import com.example.aegizpoduct.model.AppRole
import com.example.aegizpoduct.model.DemoConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
object AppSession {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val fbUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (fbUser != null) {
            _uid.value = fbUser.uid
            _email.value = fbUser.email ?: prefs().getString(KEY_EMAIL, null)
            _fullname.value = prefs().getString(KEY_FULLNAME, null) ?: fbUser.displayName ?: "Pengguna"
        } else {
            _uid.value = prefs().getString(KEY_UID, null)
            _email.value = prefs().getString(KEY_EMAIL, null)
            _fullname.value = prefs().getString(KEY_FULLNAME, null)
        }
        _role.value = prefs().getString(KEY_ROLE, null)?.let { name ->
            runCatching { AppRole.valueOf(name) }.getOrNull()
        }
        val currentUid = _uid.value ?: "default"
        _activeMissionCode.value = prefs()
            .getString(KEY_ACTIVE_MISSION + "_" + currentUid, null)
            ?.takeIf { it.isNotBlank() }
        _profilePhotoUri.value = prefs().getString(KEY_PROFILE_PHOTO + "_" + currentUid, null)
    }

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

    fun currentRescuerId(): String = _uid.value?.takeIf { it.isNotBlank() } ?: DemoConfig.RESCUER_ID
    fun currentRescuerName(): String = _fullname.value?.takeIf { it.isNotBlank() } ?: DemoConfig.RESCUER_NAME
    fun currentResponsibleId(): String = _uid.value?.takeIf { it.isNotBlank() } ?: DemoConfig.RESPONSIBLE_ID
    fun currentResponsibleName(): String = _fullname.value?.takeIf { it.isNotBlank() } ?: DemoConfig.RESPONSIBLE_NAME

    fun setUser(uid: String, fullname: String, email: String, role: AppRole) {
        _uid.value = uid
        _fullname.value = fullname
        _email.value = email
        _role.value = role
        prefs().edit()
            .putString(KEY_UID, uid)
            .putString(KEY_FULLNAME, fullname)
            .putString(KEY_EMAIL, email)
            .putString(KEY_ROLE, role.name)
            .apply()
        _profilePhotoUri.value = prefs().getString(KEY_PROFILE_PHOTO + "_" + uid, null)
        _activeMissionCode.value = prefs().getString(KEY_ACTIVE_MISSION + "_" + uid, null)
    }

    fun setRole(role: AppRole) {
        _role.value = role
        prefs().edit().putString(KEY_ROLE, role.name).apply()
    }

    fun clearRole() {
        _role.value = null
        prefs().edit().remove(KEY_ROLE).apply()
    }

    fun setActiveMission(code: String) {
        val normalized = code.trim().uppercase(Locale.US).replace(Regex("""[^A-Z0-9_-]"""), "")
        if (normalized.isBlank()) return
        _activeMissionCode.value = normalized
        val currentUid = _uid.value ?: "default"
        prefs().edit().putString(KEY_ACTIVE_MISSION + "_" + currentUid, normalized).apply()
    }

    fun clearActiveMission() {
        _activeMissionCode.value = null
        val currentUid = _uid.value ?: "default"
        prefs().edit().remove(KEY_ACTIVE_MISSION + "_" + currentUid).apply()
    }

    fun setProfilePhoto(filePath: String) {
        _profilePhotoUri.value = filePath
        val currentUid = _uid.value ?: "default"
        prefs().edit().putString(KEY_PROFILE_PHOTO + "_" + currentUid, filePath).apply()
    }

    fun clearProfilePhoto() {
        _profilePhotoUri.value = null
        val currentUid = _uid.value ?: "default"
        prefs().edit().remove(KEY_PROFILE_PHOTO + "_" + currentUid).apply()
    }

    fun clearSession() {
        runCatching { FirebaseAuth.getInstance().signOut() }
        val currentUid = _uid.value ?: "default"
        _uid.value = null
        _fullname.value = null
        _email.value = null
        _role.value = null
        _activeMissionCode.value = null
        _profilePhotoUri.value = null
        prefs().edit()
            .remove(KEY_UID)
            .remove(KEY_FULLNAME)
            .remove(KEY_EMAIL)
            .remove(KEY_ROLE)
            .apply()
    }

    private fun prefs() =
        requireNotNull(appContext) { "AppSession.initialize(context) belum dipanggil" }
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun context(): Context = requireNotNull(appContext) { "AppSession.initialize(context) belum dipanggil" }

    private const val PREFS = "aegiz_session"
    private const val KEY_ROLE = "app_role"
    private const val KEY_ACTIVE_MISSION = "active_mission_code"
    private const val KEY_UID = "auth_uid"
    private const val KEY_FULLNAME = "auth_fullname"
    private const val KEY_EMAIL = "auth_email"
    private const val KEY_PROFILE_PHOTO = "profile_photo_uri"
}