package com.withpet.car.platform.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import android.content.Context
import android.util.Log
import com.withpet.car.domain.model.UnsupportedFeature
import com.withpet.car.platform.pleosCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Pleos Vehicle SDK 어댑터.
 * - 공조(온도·풍량): 차량이 지원한다고 알려 준 모든 좌석 구역에 적용
 * - 창문: 2열(뒷좌석) 창문만, 앱의 0~100%를 차량이 알려 준 위치 범위로 변환
 */
@Singleton
class PleosVehicleControl @Inject constructor(
    @ApplicationContext private val context: Context,
) : VehicleControl {

    private val mutex = Mutex()
    private var vehicle: Vehicle? = null

    private suspend fun vehicle(): Vehicle = mutex.withLock {
        vehicle ?: withContext(Dispatchers.IO) {
            Vehicle(context.applicationContext).also { it.initialize() }
        }.also { vehicle = it }
    }

    override suspend fun setCabinTemperature(celsius: Float): Result<Unit> = runCatching {
        val hvac = vehicle().getHVAC()
        val areas = capability("공조 온도") {
            pleosCall<Capability<SeatArea, Float>> { ok, fail ->
                hvac.checkHvacTemperatureCapabilities({ config, map, _ -> ok(Capability(config, map)) }, fail)
            }
        }
        areas.keys.forEach { area ->
            pleosCall<Unit> { ok, fail ->
                hvac.setHvacTemperature(area, celsius, TemperatureUnit.CELSIUS, { _, _ -> ok(Unit) }, fail)
            }
        }
    }

    override suspend fun setFanLevel(level: Int): Result<Unit> = runCatching {
        val hvac = vehicle().getHVAC()
        val areas = capability("풍량") {
            pleosCall<Capability<SeatArea, Int>> { ok, fail ->
                hvac.checkHvacFanSpeedCapabilities({ config, map -> ok(Capability(config, map)) }, fail)
            }
        }
        areas.forEach { (area, bounds) ->
            val speed = FanLevelMapping.toSpeed(level, bounds.minValue, bounds.maxValue)
            pleosCall<Unit> { ok, fail -> hvac.setHvacFanSpeed(area, speed, { _, _ -> ok(Unit) }, fail) }
        }
    }

    override suspend fun setRearWindowVentilation(openPercent: Int): Result<Unit> = runCatching {
        val window = vehicle().getWindow()
        rearWindows(window, write = true).forEach { (area, bounds) ->
            val position = WindowPositionMapping.toPosition(openPercent, bounds.minValue, bounds.maxValue)
            pleosCall<Unit> { ok, fail -> window.setWindowPosition(area, position, { _, _ -> ok(Unit) }, fail) }
        }
    }

    override fun rearWindowOpenPercent(): Flow<Int?> = flow {
        val percent = runCatching {
            val window = vehicle().getWindow()
            val (area, bounds) = rearWindows(window, write = false).first()
            val position = pleosCall<Int?> { ok, fail -> window.getCurrentWindowPosition(area, ok, fail) }
            position?.let { WindowPositionMapping.toPercent(it, bounds.minValue, bounds.maxValue) }
        }.onFailure { Log.w(TAG, "뒷창문 위치를 읽지 못함", it) }.getOrNull()
        emit(percent)
    }

    /** 실내 현재 온도. 변경 알림 대신 주기적으로 읽는다. */
    override fun cabinTemperature(): Flow<Float?> = flow {
        val area = runCatching {
            val hvac = vehicle().getHVAC()
            pleosCall<List<SeatArea>> { ok, fail ->
                hvac.checkHvacTemperatureCurrentCapabilities({ _, areas -> ok(areas) }, fail)
            }.firstOrNull()
        }.getOrNull()
        if (area == null) {
            emit(null)
            return@flow
        }
        while (true) {
            val temp = runCatching {
                val hvac = vehicle().getHVAC()
                pleosCall<Float?> { ok, fail -> hvac.getHvacTemperatureCurrent(area, TemperatureUnit.CELSIUS, ok, fail) }
            }.getOrNull()
            emit(temp)
            delay(TEMPERATURE_POLL_MS)
        }
    }.distinctUntilChanged()

    private suspend fun rearWindows(
        window: ai.pleos.playground.vehicle.api.Window,
        write: Boolean,
    ): List<Pair<WindowArea, AreaBounds<Int>>> {
        val all = capability("창문 자동 조절", write) {
            pleosCall<Capability<WindowArea, Int>> { ok, fail ->
                window.checkWindowPositionCapabilities({ config, map -> ok(Capability(config, map)) }, fail)
            }
        }
        return all.filterKeys { area -> area.values.any { it in REAR_WINDOWS } }.toList()
            .ifEmpty { throw UnsupportedFeature("뒷창문 제어 미지원") }
    }

    private class Capability<K, T>(val config: CapabilityConfig, val areas: Map<K, AreaBounds<T>>)

    /** 기능 지원 조회가 실패하거나, 지원 구역이 없거나, 필요한 접근(쓰기/읽기)이 없으면 미지원으로 본다. */
    private suspend fun <K, T> capability(
        feature: String,
        write: Boolean = true,
        query: suspend () -> Capability<K, T>,
    ): Map<K, AreaBounds<T>> {
        val result = try {
            query()
        } catch (e: Exception) {
            throw UnsupportedFeature("$feature 미지원: ${e.message}")
        }
        val access = result.config.accessType
        val allowed = if (write) access == AccessType.WRITE || access == AccessType.READ_WRITE
        else access == AccessType.READ || access == AccessType.READ_WRITE
        if (!allowed || result.areas.isEmpty()) throw UnsupportedFeature("$feature 미지원")
        return result.areas
    }

    private companion object {
        const val TAG = "PleosVehicleControl"
        const val TEMPERATURE_POLL_MS = 30_000L
        val REAR_WINDOWS = setOf(WindowValue.ROW_2_LEFT, WindowValue.ROW_2_RIGHT)
    }
}

/**
 * 앱의 창문 개방률(0~100%)과 차량 창문 위치 값 사이 변환.
 * TODO(M9 검증): 최솟값이 완전히 닫힘이라고 가정 (Android VHAL WINDOW_POS와 같은 방식). 에뮬레이터에서 확인 필요.
 */
internal object WindowPositionMapping {
    fun toPosition(percent: Int, min: Int, max: Int): Int =
        (min + (max - min) * percent.coerceIn(0, 100) / 100f).roundToInt()

    fun toPercent(position: Int, min: Int, max: Int): Int =
        if (max == min) 0 else ((position - min) * 100f / (max - min)).roundToInt().coerceIn(0, 100)
}

/** 앱의 풍량 1~5단계를 차량 풍량 범위로 비례 변환 */
internal object FanLevelMapping {
    const val APP_MIN = 1
    const val APP_MAX = 5

    fun toSpeed(level: Int, min: Int, max: Int): Int {
        val ratio = (level.coerceIn(APP_MIN, APP_MAX) - APP_MIN).toFloat() / (APP_MAX - APP_MIN)
        return (min + (max - min) * ratio).roundToInt().coerceIn(min, max)
    }
}
