package com.withpet.car.domain.model

data class PetModeSettings(
    val targetTempC: Float = 22f,        // 기본값, 사용자 설정 가능 (수의 자문 후 확정)
    val fanLevel: Int = 2,
    /** 뒷창문 환기. 반려동물이 창밖으로 몸을 내밀거나 끼일 수 있어 기본은 꺼짐, 사용자가 켜야 쓴다 (명세 7-6). */
    val rearWindowEnabled: Boolean = false,
    val rearWindowOpenPercent: Int = 5,
    val mediaVolumePercent: Int = 30,
    val autoPlayCalmSound: Boolean = true,
) {
    companion object {
        /** 뒷창문 개방 상한. 실제 개방 폭(cm)은 차종마다 달라 실차 확인 후 조정 (명세 13장). */
        const val REAR_WINDOW_MAX_PERCENT = 10
        const val REAR_WINDOW_MIN_PERCENT = 5
    }
}

data class RestAlertSettings(
    val enabled: Boolean = true,
    val intervalMinutes: Int = 90,       // 기본값, 설정 가능
) {
    companion object {
        val INTERVAL_OPTIONS = listOf(60, 90, 120)
    }
}
