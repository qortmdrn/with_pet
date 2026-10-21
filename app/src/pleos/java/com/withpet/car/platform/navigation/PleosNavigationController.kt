package com.withpet.car.platform.navigation

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.NamedPoint
import com.withpet.car.domain.model.RouteInfo
import com.withpet.car.domain.model.UnsupportedFeature
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import ai.pleos.playground.navi.data.RouteInfo as NaviRouteInfo

/** Pleos NaviHelper SDK로 내장 내비게이션을 제어한다. */
@Singleton
class PleosNavigationController @Inject constructor(
    @ApplicationContext private val context: Context,
) : NavigationController, NaviHelperEventListener {

    private val mutex = Mutex()
    private var helper: NaviHelper? = null
    private val route = MutableStateFlow<RouteInfo?>(null)

    private suspend fun helper(): NaviHelper = mutex.withLock {
        helper ?: NaviHelper(context.applicationContext).also { created ->
            withTimeout(INIT_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    created.initialize(
                        { if (cont.isActive) cont.resume(Unit) },
                        { if (cont.isActive) cont.resumeWithException(it) },
                    )
                }
            }
            created.addListener(this)
            // 앱 시작 전부터 진행 중인 경로가 있으면 받아 온다
            created.getDestinationInfo()
            created.getWaypointInfo()
            helper = created
        }
    }

    override suspend fun startGuidance(destination: GeoPoint, name: String): Result<Unit> = runCatching {
        helper().requestRoute(
            NaviRouteInfo(
                latitude = destination.lat,
                longitude = destination.lng,
                poiId = "",
                poiName = name,
                poiSubId = "0",
                address = "",
            ),
        )
        route.value = RouteInfo(NamedPoint(destination, name), emptyList())
    }

    override suspend fun addWaypoint(point: GeoPoint, name: String): Result<Unit> = runCatching {
        val current = route.value ?: throw IllegalStateException("활성 경로 없음")
        // 내장 내비는 경유지를 최대 2곳까지 받는다
        val index = when (current.waypoints.size) {
            0 -> WaypointIndex.FIRST
            1 -> WaypointIndex.SECOND
            else -> throw UnsupportedFeature("경유지는 2곳까지 추가할 수 있어요")
        }
        helper().addWaypoint(
            RequestWaypointInfo(
                latitude = point.lat,
                longitude = point.lng,
                waypointIndex = index,
                poiId = "",
                poiName = name,
                poiSubId = "0",
                address = "",
            ),
        )
        route.update { it?.copy(waypoints = it.waypoints + NamedPoint(point, name)) }
    }

    /** 임의 목적지까지의 ETA 조회 API가 없다. 호출 측이 직선거리로 추정한다. */
    override suspend fun estimateEtaMinutes(destination: GeoPoint): Result<Int> =
        Result.failure(UnsupportedFeature("ETA 조회 미지원"))

    override fun activeRoute(): Flow<RouteInfo?> = flow {
        runCatching { helper() }.onFailure { Log.w(TAG, "NaviHelper 초기화 실패", it) }
        emitAll(route)
    }

    // --- NaviHelperEventListener ---

    override fun onDestinationInfo(destinationInfo: DestinationInfo) {
        val destination = NamedPoint(GeoPoint(destinationInfo.latitude, destinationInfo.longitude), destinationInfo.name)
        route.update { it?.copy(destination = destination) ?: RouteInfo(destination, emptyList()) }
    }

    override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>) {
        route.update { current ->
            current?.copy(waypoints = waypointInfoList.map { NamedPoint(GeoPoint(it.latitude, it.longitude), it.name) })
        }
    }

    override fun onDestinationChanged() {
        helper?.getDestinationInfo()
    }

    override fun onWaypointChanged(waypointChangedInfo: ai.pleos.playground.navi.data.WaypointChangedInfo) {
        helper?.getWaypointInfo()
    }

    override fun onRouteCancelled() {
        route.value = null
    }

    override fun onDestinationArrived(destinationArrivedInfo: ai.pleos.playground.navi.data.DestinationArrivedInfo) {
        route.value = null
    }

    override fun onNavigationStatus(status: NavigationStatus) {
        // 경로 안내가 아닌 상태(비활성·자유 주행)면 활성 경로 없음
        if (status == NavigationStatus.INACTIVE || status == NavigationStatus.FREE_DRIVE) route.value = null
    }

    override fun onError(errorCode: NaviErrorCode) {
        Log.w(TAG, "내비 오류: $errorCode")
    }

    private companion object {
        const val TAG = "PleosNavigation"
        const val INIT_TIMEOUT_MS = 5_000L
    }
}
