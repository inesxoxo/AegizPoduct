package com.example.aegizpoduct.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aegizpoduct.logic.BleManager
import com.example.aegizpoduct.logic.FirebaseConfig
import com.example.aegizpoduct.logic.FirebaseRestClient
import com.example.aegizpoduct.logic.createMission
import com.example.aegizpoduct.logic.evaluateRisk
import com.example.aegizpoduct.logic.finishMission
import com.example.aegizpoduct.logic.getMissionMeta
import com.example.aegizpoduct.logic.joinMission
import com.example.aegizpoduct.logic.markMemberSos
import com.example.aegizpoduct.logic.pollAllMissions
import com.example.aegizpoduct.logic.pollGarminHealth
import com.example.aegizpoduct.logic.pollMembers
import com.example.aegizpoduct.logic.pollSosEvents
import com.example.aegizpoduct.logic.reportMember
import com.example.aegizpoduct.logic.resolveMember
import com.example.aegizpoduct.logic.sendSosEventToFirebase
import com.example.aegizpoduct.logic.SosOfflineQueue
import com.example.aegizpoduct.Model.*
import com.example.aegizpoduct.session.AppSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.example.aegizpoduct.ui.*

class BleViewModel(application: Application) : AndroidViewModel(application) {
    val manager = BleManager(application)
    val state: StateFlow<BleUiState> = manager.state

    fun startScan() = manager.startScan()
    fun disconnect() = manager.disconnect()
    fun onPermissionDenied() = manager.onPermissionDenied()
    fun dismissSos() = manager.dismissSos()
    fun startLocationUpdates() = manager.startLocationTracking() // start location updates on phone
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val client = FirebaseRestClient()
    private val offlineQueue = SosOfflineQueue(application)

    private val _state = MutableStateFlow(Esp32UiState(garmin = null))
    val state: StateFlow<Esp32UiState> = _state.asStateFlow()

    private var pollingStarted = false

    fun startPolling(missionCode: String? = null) {
        if (pollingStarted) return
        pollingStarted = true
        viewModelScope.launch {
            while (true) {
                runCatching { pollOnce() }
                delay(1_500L)
            }
        }
    }

    private suspend fun pollOnce() {
        if (offlineQueue.count() > 0) {
            val queued = offlineQueue.all()
            for (ev in queued) {
                val sent = runCatching { sendSosEventToFirebase(client, ev) }.getOrDefault(false)
                if (sent) {
                    offlineQueue.remove(ev.eventId)
                    runCatching {
                        markMemberSos(client, ev.missionId, ev.rescuerId, ev.rescuerName, ev.lat, ev.lon)
                    }
                }
            }
        }
        val garmin = runCatching { pollGarminHealth(client) }.getOrNull()
        val effectiveGarmin = garmin ?: _state.value.garmin
        val code = AppSession.activeMissionCode.value
        val history = runCatching { pollAllMissions(client) }.getOrDefault(_state.value.missionHistory)

        if (code.isNullOrBlank()) {
            _state.update {
                it.copy(
                    activeMissionCode = null,
                    missionMeta = null,
                    members = emptyList(),
                    sosEvents = emptyList(),
                    garmin = effectiveGarmin,
                    missionHistory = history
                )
            }
            return
        }

        val remoteMeta = runCatching { getMissionMeta(client, code) }.getOrNull()
        if (remoteMeta != null && !remoteMeta.status.equals("active", ignoreCase = true)) {
            AppSession.clearActiveMission()
            val msg = if (AppSession.role.value == AppRole.RESCUER) "Misi telah diselesaikan oleh Posko" else "Misi telah selesai"
            _state.update {
                it.copy(
                    activeMissionCode = null,
                    missionMeta = null,
                    members = emptyList(),
                    sosEvents = emptyList(),
                    missionHistory = history,
                    missionMessage = msg,
                )
            }
            return
        }

        val remoteMembers = runCatching { pollMembers(client, code) }.getOrDefault(emptyList())
        val events = runCatching { pollSosEvents(client, code) }.getOrDefault(emptyList())

        if (AppSession.activeMissionCode.value != code) return

        if (AppSession.role.value == AppRole.RESCUER) {
            val myId = AppSession.currentRescuerId()
            val isMember = remoteMembers.any { it.rescuerId == myId }
            val panicActive = events.any {
                it.status.equals("DARURAT", ignoreCase = true) && it.rescuerId == myId
            }
            val assessment = if (isMember) {
                runCatching {
                    reportMember(
                        client = client,
                        code = code,
                        health = effectiveGarmin,
                        panicActive = panicActive,
                        lat = null,
                        lon = null,
                        rescuerId = myId,
                        rescuerName = AppSession.currentRescuerName(),
                    )
                }.getOrNull() ?: evaluateRisk(effectiveGarmin, panicActive)
            } else {
                evaluateRisk(effectiveGarmin, panicActive)
            }

            val mergedMembers = if (isMember) {
                val myMember = MissionMember(
                    rescuerId = myId,
                    name = AppSession.currentRescuerName(),
                    status = assessment.status.label,
                    riskScore = assessment.score,
                    riskStatus = assessment.status.name,
                    updatedAt = System.currentTimeMillis() / 1000
                )
                val list = remoteMembers.toMutableList()
                val idx = list.indexOfFirst { it.rescuerId == myId }
                if (idx >= 0) list[idx] = myMember else list.add(myMember)
                list
            } else {
                remoteMembers
            }

            _state.update {
                it.copy(
                    activeMissionCode = code,
                    missionMeta = remoteMeta ?: it.missionMeta,
                    members = mergedMembers,
                    sosEvents = events,
                    garmin = effectiveGarmin,
                    missionHistory = history,
                )
            }
        } else {
            val finalMembers = if (remoteMembers.isNotEmpty()) remoteMembers else _state.value.members
            _state.update {
                it.copy(
                    activeMissionCode = code,
                    missionMeta = remoteMeta ?: it.missionMeta,
                    members = finalMembers,
                    sosEvents = events,
                    garmin = effectiveGarmin,
                    missionHistory = history,
                )
            }
        }
    }

    fun restoreActiveMission() {
        viewModelScope.launch {
            val restoredCode = AppSession.activeMissionCode.value
            if (!restoredCode.isNullOrBlank()) {
                startPolling(restoredCode)
                runCatching { pollOnce() }
            } else {
                startPolling(null)
                runCatching { pollOnce() }
            }
        }
    }

    fun joinMission(code: String) {
        viewModelScope.launch {
            val missionCode = code.trim().uppercase()
            _state.update { it.copy(missionMessage = null, missionError = null) }
            runCatching {
                joinMission(
                    client = client,
                    code = missionCode,
                    rescuerId = AppSession.currentRescuerId(),
                    rescuerName = AppSession.currentRescuerName(),
                )
            }.onSuccess { remoteMeta ->
                AppSession.setActiveMission(missionCode)
                _state.update {
                    it.copy(
                        activeMissionCode = missionCode,
                        missionMeta = remoteMeta,
                        missionMessage = "Berhasil gabung misi $missionCode",
                        missionError = null
                    )
                }
                startPolling(missionCode)
                runCatching { pollOnce() }
            }.onFailure { e ->
                _state.update {
                    it.copy(missionError = e.message ?: "Gagal gabung misi. Cek koneksi internet.")
                }
            }
        }
    }

    fun createMission(code: String, title: String, desc: String, cat: String, lat: Double? = null, lon: Double? = null) {
        viewModelScope.launch {
            _state.update { it.copy(missionMessage = null, missionError = null) }
            runCatching {
                createMission(
                    client = client,
                    code = code,
                    title = title,
                    description = desc,
                    category = cat,
                    createdBy = AppSession.currentResponsibleId(),
                    lat = lat,
                    lon = lon,
                )
            }.onSuccess { meta ->
                AppSession.setActiveMission(meta.code)
                _state.update {
                    it.copy(
                        activeMissionCode = meta.code,
                        missionMeta = meta,
                        missionMessage = "Misi ${meta.code} dibuat",
                        missionError = null
                    )
                }
                startPolling(meta.code)
                runCatching { pollOnce() }
            }.onFailure { e ->
                _state.update {
                    it.copy(missionError = e.message ?: "Gagal membuat misi. Cek koneksi internet.")
                }
            }
        }
    }

    fun finishMission() {
        viewModelScope.launch {
            val code = AppSession.activeMissionCode.value ?: return@launch
            AppSession.clearActiveMission()
            _state.update {
                it.copy(
                    activeMissionCode = null,
                    missionMeta = null,
                    members = emptyList(),
                    sosEvents = emptyList(),
                    missionMessage = "Misi selesai",
                    missionError = null
                )
            }
            runCatching {
                finishMission(client, code)
            }
        }
    }

    fun forwardBleSos(event: SosEvent) {
        viewModelScope.launch {
            val success = runCatching { sendSosEventToFirebase(client, event) }.getOrDefault(false)
            if (!success) {
                offlineQueue.enqueue(event)
            }
            runCatching {
                markMemberSos(client, event.missionId, event.rescuerId, event.rescuerName, event.lat, event.lon)
            }
        }
    }

    fun resolveMember(rescuerId: String) {
        viewModelScope.launch {
            val code = AppSession.activeMissionCode.value ?: return@launch
            runCatching {
                resolveMember(client, code, rescuerId)
            }
        }
    }

    fun triggerSos(event: SosEvent) {
        viewModelScope.launch {
            val success = runCatching { sendSosEventToFirebase(client, event) }.getOrDefault(false)
            if (!success) {
                offlineQueue.enqueue(event)
            }
            runCatching {
                markMemberSos(client, event.missionId, event.rescuerId, event.rescuerName, event.lat, event.lon)
            }
        }
    }
}