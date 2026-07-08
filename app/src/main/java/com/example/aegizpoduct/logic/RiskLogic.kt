package com.example.aegizpoduct.logic

import com.example.aegizpoduct.Model.GarminHealth
import com.example.aegizpoduct.Model.RiskAssessment
import com.example.aegizpoduct.Model.RiskStatus
import kotlin.math.roundToInt

private const val AMAN_THRESHOLD = 40
private const val WASPADA_THRESHOLD = 70
private const val WEIGHT_HEART_RATE = 0.36
private const val WEIGHT_BODY_BATTERY = 0.29
private const val WEIGHT_RESPIRATION = 0.21
private const val WEIGHT_STRESS = 0.14
private const val LOW_BODY_BATTERY_THRESHOLD = 20
private const val VERY_LOW_BODY_BATTERY_THRESHOLD = 10
private const val LOW_BODY_BATTERY_RISK_FLOOR = AMAN_THRESHOLD
private const val VERY_LOW_BODY_BATTERY_RISK_FLOOR = 55

fun evaluateRisk(health: GarminHealth?, panicActive: Boolean = false): RiskAssessment {
    if (panicActive) {
        return RiskAssessment(100, RiskStatus.DARURAT, "SOS aktif")
    }
    val effectiveHealth = health ?: GarminHealth(
        heartRate = 78,
        bodyBattery = 85,
        stress = 22,
        respiration = 16,
        spo2 = 98,
        updatedAt = System.currentTimeMillis() / 1000
    )

    val critical = criticalReason(effectiveHealth)
    val score = applyRiskFloors(effectiveHealth, weightedScore(effectiveHealth))
    val status = when {
        critical != null -> RiskStatus.DARURAT
        score < AMAN_THRESHOLD -> RiskStatus.AMAN
        score < WASPADA_THRESHOLD -> RiskStatus.WASPADA
        else -> RiskStatus.BAHAYA
    }
    return RiskAssessment(
        score = if (critical != null) 100 else score,
        status = status,
        reason = critical ?: floorReason(effectiveHealth) ?: dominantReason(effectiveHealth),
    )
}

private fun weightedScore(health: GarminHealth): Int {
    val weighted =
        vitalScoreHeartRate(health.heartRate) * WEIGHT_HEART_RATE +
                vitalScoreBodyBattery(health.bodyBattery) * WEIGHT_BODY_BATTERY +
                vitalScoreRespiration(health.respiration) * WEIGHT_RESPIRATION +
                vitalScoreStress(health.stress) * WEIGHT_STRESS
    return weighted.roundToInt().coerceIn(0, 100)
}

private fun applyRiskFloors(health: GarminHealth, score: Int): Int {
    val bodyBattery = health.bodyBattery ?: return score
    val floor = when {
        bodyBattery <= VERY_LOW_BODY_BATTERY_THRESHOLD -> VERY_LOW_BODY_BATTERY_RISK_FLOOR
        bodyBattery <= LOW_BODY_BATTERY_THRESHOLD -> LOW_BODY_BATTERY_RISK_FLOOR
        else -> 0
    }
    return score.coerceAtLeast(floor)
}

private fun floorReason(health: GarminHealth): String? {
    val bodyBattery = health.bodyBattery ?: return null
    return when {
        bodyBattery <= VERY_LOW_BODY_BATTERY_THRESHOLD -> "Body battery sangat rendah"
        bodyBattery <= LOW_BODY_BATTERY_THRESHOLD -> "Body battery rendah"
        else -> null
    }
}

private fun criticalReason(health: GarminHealth): String? = when {
    (health.heartRate ?: 80) < 40 -> "Heart rate terlalu rendah"
    (health.heartRate ?: 80) > 190 -> "Heart rate terlalu tinggi"
    (health.respiration ?: 16) < 8 -> "Napas terlalu rendah"
    (health.respiration ?: 16) > 35 -> "Napas terlalu tinggi"
    else -> null
}

private fun dominantReason(health: GarminHealth): String {
    val scores = listOf(
        "Heart rate" to vitalScoreHeartRate(health.heartRate),
        "Body battery" to vitalScoreBodyBattery(health.bodyBattery),
        "Respiration" to vitalScoreRespiration(health.respiration),
        "Stress" to vitalScoreStress(health.stress),
    )
    val (name, score) = scores.maxBy { it.second }
    return if (score <= 25) "Kondisi stabil" else "$name perlu dipantau"
}

private fun vitalScoreHeartRate(value: Int?): Int {
    val hr = value ?: return 25
    return when {
        hr in 60..100 -> 0
        hr in 101..140 -> 25
        hr in 141..170 -> 50
        hr in 40..59 || hr in 171..190 -> 75
        else -> 100
    }
}

private fun vitalScoreBodyBattery(value: Int?): Int {
    val bb = value ?: return 25
    return when {
        bb > 80 -> 0
        bb >= 61 -> 25
        bb >= 41 -> 50
        bb >= 21 -> 75
        else -> 100
    }
}

private fun vitalScoreRespiration(value: Int?): Int {
    val rr = value ?: return 25
    return when {
        rr in 12..20 -> 0
        rr in 21..24 -> 25
        rr in 25..29 -> 50
        rr in 8..11 || rr in 30..34 -> 75
        else -> 100
    }
}

private fun vitalScoreStress(value: Int?): Int {
    val stress = value ?: return 25
    return when {
        stress <= 25 -> 0
        stress <= 50 -> 25
        stress <= 75 -> 50
        stress <= 90 -> 75
        else -> 100
    }
}