package com.withpet.car.ui.walk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.withpet.car.domain.model.RouteInfo
import com.withpet.car.domain.usecase.DrivingRestrictionMonitor
import com.withpet.car.domain.usecase.GetNearbyWalkSpotsUseCase
import com.withpet.car.domain.usecase.NearbyWalkSpot
import com.withpet.car.domain.usecase.WalkSpotFilter
import com.withpet.car.platform.location.LocationProvider
import com.withpet.car.platform.navigation.NavigationController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalkUiState(
    val spots: List<NearbyWalkSpot>? = null,   // null이면 불러오는 중
    val filter: WalkSpotFilter = WalkSpotFilter.ALL,
    val route: RouteInfo? = null,
    val restricted: Boolean = false,
    val message: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WalkViewModel @Inject constructor(
    location: LocationProvider,
    restriction: DrivingRestrictionMonitor,
    private val getNearby: GetNearbyWalkSpotsUseCase,
    private val navigation: NavigationController,
) : ViewModel() {

    private val filter = MutableStateFlow(WalkSpotFilter.ALL)
    private val message = MutableStateFlow<String?>(null)

    // 주행 중에는 필터 없이 가장 가까운 곳만 (명세 7-3)
    private val effectiveFilter = combine(filter, restriction.isRestricted) { f, restricted ->
        if (restricted) WalkSpotFilter.ALL else f
    }.distinctUntilChanged()

    private val spots: Flow<List<NearbyWalkSpot>?> =
        combine(location.currentLocation(), effectiveFilter) { here, f -> here to f }
            .mapLatest { (here, f) -> getNearby(here, f) }

    val state: StateFlow<WalkUiState> = combine(
        spots.onStart { emit(null) },
        filter,
        navigation.activeRoute().onStart { emit(null) },
        restriction.isRestricted,
        message,
    ) { spots, f, route, restricted, msg ->
        WalkUiState(spots, f, route, restricted, msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WalkUiState())

    fun setFilter(value: WalkSpotFilter) {
        filter.value = value
    }

    /** 경로가 있으면 경유지로 추가, 없으면 길 안내 시작 */
    fun go(item: NearbyWalkSpot) {
        viewModelScope.launch {
            val spot = item.spot
            val hasRoute = state.value.route != null
            val result = if (hasRoute) navigation.addWaypoint(spot.location, spot.name)
            else navigation.startGuidance(spot.location, spot.name)
            message.value = when {
                result.isFailure -> "길 안내를 시작하지 못했어요"
                hasRoute -> "${spot.name}을 경유지로 추가했어요"
                else -> "${spot.name}으로 길 안내를 시작했어요"
            }
        }
    }
}
