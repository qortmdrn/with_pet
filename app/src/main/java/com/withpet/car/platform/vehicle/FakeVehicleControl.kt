package com.withpet.car.platform.vehicle

import com.withpet.car.domain.model.UnsupportedFeature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 설정값을 메모리에 저장하고 Flow로 노출한다. 디버그 메뉴에서 창문 미지원 차량을 흉내 낼 수 있다. */
@Singleton
class FakeVehicleControl @Inject constructor() : VehicleControl {

    private val temperature = MutableStateFlow<Float?>(25f)
    private val fan = MutableStateFlow(1)
    private val window = MutableStateFlow<Int?>(0)
    private val windowSupported = MutableStateFlow(true)
    private val raining = MutableStateFlow(false)
    private val windowLock = MutableStateFlow(false)

    val fanLevel: StateFlow<Int> = fan.asStateFlow()
    val rearWindow: StateFlow<Int?> = window.asStateFlow()
    val isWindowSupported: StateFlow<Boolean> = windowSupported.asStateFlow()
    val isRainingNow: StateFlow<Boolean> = raining.asStateFlow()
    val rearWindowLock: StateFlow<Boolean> = windowLock.asStateFlow()

    override suspend fun setCabinTemperature(celsius: Float): Result<Unit> {
        temperature.value = celsius
        return Result.success(Unit)
    }

    override suspend fun setFanLevel(level: Int): Result<Unit> {
        fan.value = level
        return Result.success(Unit)
    }

    override suspend fun setRearWindowVentilation(openPercent: Int): Result<Unit> {
        if (!windowSupported.value) return Result.failure(UnsupportedFeature("창문 제어 미지원"))
        window.value = openPercent.coerceIn(0, 100)
        return Result.success(Unit)
    }

    override fun cabinTemperature(): Flow<Float?> = temperature.asStateFlow()

    override fun rearWindowOpenPercent(): Flow<Int?> =
        if (windowSupported.value) window.asStateFlow() else MutableStateFlow(null)

    override suspend fun isRaining(): Boolean = raining.value

    override suspend fun setRearWindowLock(locked: Boolean): Result<Unit> {
        if (!windowSupported.value) return Result.failure(UnsupportedFeature("창문 잠금 미지원"))
        windowLock.value = locked
        return Result.success(Unit)
    }

    override suspend fun rearWindowLocked(): Boolean? = if (windowSupported.value) windowLock.value else null

    fun setRaining(value: Boolean) {
        raining.value = value
    }

    fun setWindowSupported(supported: Boolean) {
        windowSupported.value = supported
    }

    fun setRearWindowManually(percent: Int) {
        window.value = percent
    }
}
