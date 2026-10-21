package com.withpet.car.platform.vehicle

import kotlinx.coroutines.flow.Flow

interface VehicleControl {
    suspend fun setCabinTemperature(celsius: Float): Result<Unit>
    suspend fun setFanLevel(level: Int): Result<Unit>
    suspend fun setRearWindowVentilation(openPercent: Int): Result<Unit>  // 0~100
    fun cabinTemperature(): Flow<Float?>

    /** 펫 모드 OFF 시 창문 복원용 현재 개방률. 알 수 없으면 null. (명세 5장 확장) */
    fun rearWindowOpenPercent(): Flow<Int?>
}
