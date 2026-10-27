package com.withpet.car.platform.vehicle

import kotlinx.coroutines.flow.Flow

interface VehicleControl {
    suspend fun setCabinTemperature(celsius: Float): Result<Unit>
    suspend fun setFanLevel(level: Int): Result<Unit>
    suspend fun setRearWindowVentilation(openPercent: Int): Result<Unit>  // 0~100
    fun cabinTemperature(): Flow<Float?>

    /** 펫 모드가 연 뒷창문 복원용 현재 개방률. 알 수 없으면 null. (명세 5장 확장) */
    fun rearWindowOpenPercent(): Flow<Int?>

    /** 앞 와이퍼 작동 상태로 추정한 비 여부. 비 감지 센서 API가 없어 간접 추정이다. 알 수 없으면 null. (명세 7-6) */
    suspend fun isRaining(): Boolean?

    /** 뒷창문 잠금(뒷좌석 창문 스위치 잠금). 반려동물이 스위치를 밟아 창문이 더 열리는 것을 막는다. 미지원이면 [com.withpet.car.domain.model.UnsupportedFeature]. */
    suspend fun setRearWindowLock(locked: Boolean): Result<Unit>

    /** 현재 뒷창문 잠금 여부. 알 수 없으면 null. */
    suspend fun rearWindowLocked(): Boolean?
}
