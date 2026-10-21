package com.withpet.car.domain.model

data class PetModeSettings(
    val targetTempC: Float = 22f,        // 기본값, 사용자 설정 가능 (수의 자문 후 확정)
    val fanLevel: Int = 2,
    val rearWindowOpenPercent: Int = 10,
    val mediaVolumePercent: Int = 30,
    val autoPlayCalmSound: Boolean = true,
)

data class RestAlertSettings(
    val enabled: Boolean = true,
    val intervalMinutes: Int = 90,       // 기본값, 설정 가능
) {
    companion object {
        val INTERVAL_OPTIONS = listOf(60, 90, 120)
    }
}
