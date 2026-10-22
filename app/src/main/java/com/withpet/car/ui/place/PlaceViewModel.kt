package com.withpet.car.ui.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.withpet.car.domain.model.PetPlaceCategory
import com.withpet.car.domain.model.RouteInfo
import com.withpet.car.domain.usecase.DrivingRestrictionMonitor
import com.withpet.car.domain.usecase.GetNearbyPetPlacesUseCase
import com.withpet.car.domain.usecase.NearbyPetPlace
import com.withpet.car.domain.usecase.NearbyPetPlaces
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

data class PlaceUiState(
    val result: NearbyPetPlaces? = null,   // null이면 불러오는 중
    val category: PetPlaceCategory = PetPlaceCategory.CAFE,
    val route: RouteInfo? = null,
    val restricted: Boolean = false,
    val message: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaceViewModel @Inject constructor(
    location: LocationProvider,
    restriction: DrivingRestrictionMonitor,
    private val getNearby: GetNearbyPetPlacesUseCase,
    private val navigation: NavigationController,
) : ViewModel() {

    private val category = MutableStateFlow(PetPlaceCategory.CAFE)
    private val message = MutableStateFlow<String?>(null)

    // 주행 중 위치가 바뀔 때마다 목록 순서가 흔들리지 않도록 일정 거리 이상 움직였을 때만 다시 계산한다
    private val origin = location.currentLocation()
        .distinctUntilChanged { old, new -> old.distanceKmTo(new) < REFRESH_DISTANCE_KM }

    private val result: Flow<NearbyPetPlaces?> =
        combine(origin, category) { here, c -> here to c }
            .mapLatest { (here, c) -> getNearby(here, c) }

    val state: StateFlow<PlaceUiState> = combine(
        result.onStart { emit(null) },
        category,
        navigation.activeRoute().onStart { emit(null) },
        restriction.isRestricted,
        message,
    ) { result, c, route, restricted, msg ->
        // 종류를 바꾼 직후 이전 종류의 목록이 보이지 않게 한다
        PlaceUiState(result?.takeIf { it.places.all { p -> p.place.category == c } }, c, route, restricted, msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaceUiState())

    fun setCategory(value: PetPlaceCategory) {
        category.value = value
        message.value = null
    }

    /** 경로가 있으면 경유지로 추가, 없으면 길 안내 시작 */
    fun go(item: NearbyPetPlace) {
        viewModelScope.launch {
            val place = item.place
            val hasRoute = state.value.route != null
            val result = if (hasRoute) navigation.addWaypoint(place.location, place.name)
            else navigation.startGuidance(place.location, place.name)
            message.value = when {
                result.isFailure -> "길 안내를 시작하지 못했어요"
                hasRoute -> "${place.name}을 경유지로 추가했어요"
                else -> "${place.name}으로 길 안내를 시작했어요"
            }
        }
    }

    /** 앱 목록에 없는 곳은 내장 내비 검색 화면으로 넘긴다 */
    fun searchInNavigation() {
        // 누른 시점의 종류로 검색한다
        val keyword = GetNearbyPetPlacesUseCase.naviKeyword(category.value)
        viewModelScope.launch {
            val result = navigation.searchPlace(keyword)
            if (result.isFailure) message.value = "내비 검색을 열지 못했어요"
        }
    }

    private companion object {
        const val REFRESH_DISTANCE_KM = 0.3
    }
}
