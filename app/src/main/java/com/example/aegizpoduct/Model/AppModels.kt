package com.example.aegizpoduct.Model

import androidx.privacysandbox.ads.adservices.adid.AdId
import java.util.UUID

enum class AppRole {RESCUER, PENANGGUNG_JAWAB}

data class AppUser (
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val role: String = "",
    val createdAt: Long = 0L,
)

data class DemoAccount(
    val username: String,
    val password: String,
    val role: AppRole,
    val userId: String,
    val displayName: String,
)

object DemoConfig{
    const val MISSION_ID = "DEMO-01"
    const val MISSION_NAME = "Misi 01"
    const val RESPONSIBLE_ID = "PJ001"
    const val RESPONSIBLE_NAME = "Penanggung Jawab 01"
    const val RESCUER_ID = "R001"
    const val RESCUER_NAME = "Rescuer 01"
    const val DEVICE_ID = "DEV001"

    val account = listOf(
        DemoAccount(
            username = "penanggungJawab01",
            password = "demo123",
            role = AppRole.PENANGGUNG_JAWAB,
            userId = RESPONSIBLE_ID,
            displayName = RESPONSIBLE_NAME
        ),
        DemoAccount(
            username = "Rescuer01",
            password = "demo123",
            role = AppRole.RESCUER,
            userId = RESCUER_ID,
            displayName = RESCUER_NAME,
        ),
    )
}
