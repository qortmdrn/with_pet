package com.withpet.car.platform.navigation

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.TripInfo
import ai.pleos.playground.navi.data.TripRequest
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
import kotlinx.coroutines.CompletableDeferred
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
    // requestTrip 응답에는 요청 식별자가 없어 한 번에 하나씩만 요청한다
    private val tripMutex = Mutex()
    @Volatile private var pendingTrip: CompletableDeferred<TripInfo>? = null

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

    /**
     * requestTrip → onTripInfo로 임의 목적지까지 예상 소요 시간을 받는다 (docs/pleos-sdk/navihelper.md 4-2).
     * TripInfo.eta는 "초 단위 ETA"라고만 되어 있어, 소요 시간이 아니라 시각(epoch 초)이면 현재 시각과의 차이로 바꾼다.
     */
    override suspend fun estimateEtaMinutes(destination: GeoPoint): Result<Int> = runCatching {
        tripMutex.withLock {
            val trip = CompletableDeferred<TripInfo>()
            pendingTrip = trip
            try {
                helper().requestTrip(
                    TripRequest(latitude = destination.lat, longitude = destination.lng, poiId = "", poiName = "", poiSubId = "0"),
                )
                etaMinutes(withTimeout(TRIP_TIMEOUT_MS) { trip.await() }.eta)
            } finally {
                pendingTrip = null
            }
        }
    }

    private fun etaMinutes(eta: Int): Int {
        val seconds = if (eta > EPOCH_THRESHOLD_S) eta - System.currentTimeMillis() / 1000 else eta.toLong()
        if (seconds !in 0..MAX_TRIP_S) throw IllegalStateException("ETA 값 이상: $eta")
        return ((seconds + 59) / 60).toInt().coerceAtLeast(1)
    }

    override suspend fun searchPlace(keyword: String): Result<Unit> = runCatching {
        helper().requestNavigationScreen(NavigationScreenRequest.SearchPlace(keyword))
    }

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

    override fun onTripInfo(tripInfo: TripInfo) {
        pendingTrip?.complete(tripInfo)
    }

    override fun onError(errorCode: NaviErrorCode) {
        Log.w(TAG, "내비 오류: $errorCode")
        if (errorCode == NaviErrorCode.CAUSE_SEARCH_TRIP) pendingTrip?.completeExceptionally(IllegalStateException("경로 시간 조회 실패"))
    }

    private companion object {
        const val TAG = "PleosNavigation"
        const val INIT_TIMEOUT_MS = 5_000L
        const val TRIP_TIMEOUT_MS = 3_000L
        // 이보다 크면 소요 초가 아니라 epoch 초로 본다 (약 3년)
        const val EPOCH_THRESHOLD_S = 100_000_000
        const val MAX_TRIP_S = 2L * 24 * 3600
    }
}
