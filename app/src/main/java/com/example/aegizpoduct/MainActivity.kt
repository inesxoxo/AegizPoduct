package com.example.aegizpoduct

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import com.example.aegizpoduct.model.AppRole
import com.example.aegizpoduct.model.SosEvent
import com.example.aegizpoduct.model.DemoConfig
import com.example.aegizpoduct.session.AppSession
import com.example.aegizpoduct.ui.AegizRescuerApp
import com.example.aegizpoduct.ui.AegizSupervisorApp
import com.example.aegizpoduct.ui.AuthScreen
import com.example.aegizpoduct.ui.BleViewModel
import com.example.aegizpoduct.ui.DashboardViewModel

class MainActivity : ComponentActivity() {
    private val bleViewModel by viewModels<BleViewModel>()
    private val dashboardViewModel by viewModels<DashboardViewModel>()

    private val requiredBlePermissions = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        else -> arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private val allStartupPermissions = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> requiredBlePermissions + arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        else -> requiredBlePermissions
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (hasBlePermissions()) {
            bleViewModel.startScan()
            bleViewModel.startLocationUpdates()
        }
    }

    private val enableBtLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        bleViewModel.startScan()
        bleViewModel.startLocationUpdates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSession.initialize(this)
        dashboardViewModel.startPolling(AppSession.activeMissionCode.value)

        if (!hasAllPermissions()) {
            permissionLauncher.launch(allStartupPermissions)
        }

        setContent {
            AegizPoductTheme {
                val bleState by bleViewModel.state.collectAsState()
                val firebaseState by dashboardViewModel.state.collectAsState()
                val currentRole by AppSession.role.collectAsState()

                LaunchedEffect(Unit) {
                    if (hasBlePermissions()) {
                        bleViewModel.startLocationUpdates()
                    }
                }

                val handleScan: () -> Unit = {
                    if (!hasBlePermissions()) {
                        permissionLauncher.launch(requiredBlePermissions)
                    } else {
                        bleViewModel.startScan()
                    }
                }

                val handleEnableBt: () -> Unit = {
                    if (!hasBlePermissions()) {
                        permissionLauncher.launch(requiredBlePermissions)
                    } else {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        runCatching { enableBtLauncher.launch(enableBtIntent) }
                    }
                }

                LaunchedEffect(bleState.telemetry?.sosStartMs) {
                    val sosStartMs = bleState.telemetry?.sosStartMs ?: return@LaunchedEffect
                    val t = bleState.telemetry ?: return@LaunchedEffect
                    val missionId = firebaseState.activeMissionCode ?: DemoConfig.MISSION_ID
                    val senderId = t.sosSender ?: AppSession.currentRescuerId()
                    // Nama harus ikut rescuer pengirim SOS yang sebenarnya (dicari dari roster misi),
                    // bukan AppSession.currentRescuerName() milik HP yang sedang me-relay paket ini.
                    val senderName = firebaseState.members.firstOrNull { it.rescuerId == senderId }
                        ?.name?.takeIf { it.isNotBlank() }
                        ?: if (senderId == AppSession.currentRescuerId()) AppSession.currentRescuerName() else senderId
                    dashboardViewModel.forwardBleSos(
                        SosEvent(
                            rescuerId = senderId,
                            rescuerName = senderName,
                            deviceId = t.sosDeviceId ?: DemoConfig.DEVICE_ID,
                            missionId = missionId,
                            status = "DARURAT",
                            lat = t.sosLat ?: t.lat ?: -7.795580,
                            lon = t.sosLon ?: t.lng ?: 110.369490,
                            source = t.sosSource ?: "BLE_RELAY",
                            createdAt = sosStartMs,
                            sosPacketTimestamp = t.sosPacketTimestamp,
                        )
                    )
                }

                if (currentRole == null) {
                    AuthScreen(
                        onLogin = { AppSession.setRole(it) },
                        onLoginSuccess = { dashboardViewModel.restoreActiveMission() }
                    )
                } else if (currentRole == AppRole.RESCUER) {
                    AegizRescuerApp(
                        state = bleState,
                        firebaseState = firebaseState,
                        onScan = handleScan,
                        onDisconnect = { bleViewModel.disconnect() },
                        onEnableBluetooth = handleEnableBt,
                        onSos = {
                            val event = SosEvent(
                                rescuerId = AppSession.currentRescuerId(),
                                rescuerName = AppSession.currentRescuerName(),
                                deviceId = bleState.telemetry?.sosDeviceId ?: DemoConfig.DEVICE_ID,
                                missionId = firebaseState.activeMissionCode ?: DemoConfig.MISSION_ID,
                                status = "DARURAT",
                                lat = bleState.phoneLat ?: bleState.telemetry?.lat ?: -7.795580,
                                lon = bleState.phoneLon ?: bleState.telemetry?.lng ?: 110.369490,
                                source = "APP_SOS",
                                createdAt = System.currentTimeMillis()
                            )
                            dashboardViewModel.triggerSos(event)
                        },
                        onJoinMission = { dashboardViewModel.joinMission(it) },
                        onChangeRole = { AppSession.clearSession() }
                    )
                } else {
                    AegizSupervisorApp(
                        firebaseState = firebaseState,
                        bleState = bleState,
                        onScan = handleScan,
                        onDisconnect = { bleViewModel.disconnect() },
                        onEnableBluetooth = handleEnableBt,
                        onClearSos = { bleViewModel.dismissSos() },
                        onCreateMission = { c, t, d, cat, lat, lon -> dashboardViewModel.createMission(c, t, d, cat, lat, lon) },
                        onFinishMission = { dashboardViewModel.finishMission() },
                        onResolveMember = { dashboardViewModel.resolveMember(it) },
                        onChangeRole = { AppSession.clearSession() }
                    )
                }
            }
        }
    }

    private fun hasAllPermissions(): Boolean {
        return allStartupPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasBlePermissions(): Boolean {
        return requiredBlePermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
