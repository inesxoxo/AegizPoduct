package com.example.aegizpoduct

import android.content.Context
import com.example.aegizpoduct.Model.AppRole
import com.example.aegizpoduct.Model.DemoConfig
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
        // Muat URI foto profil yang tersimpan sebelumnya
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

    fun context(): Context = requireNotNull(appContext) { "AppSession.initialize(context) belum dipanggil" }

    private const val PREFS = "aegiz_session"
    private const val KEY_ROLE = "app_role"
    private const val KEY_ACTIVE_MISSION = "active_mission_code"
    private const val KEY_UID = "auth_uid"
    private const val KEY_FULLNAME = "auth_fullname"
    private const val KEY_EMAIL = "auth_email"
    private const val KEY_PROFILE_PHOTO = "profile_photo_uri"
}