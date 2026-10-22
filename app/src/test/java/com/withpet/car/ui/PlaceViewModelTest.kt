package com.withpet.car.ui

import com.withpet.car.CITY_HALL
import com.withpet.car.domain.model.PetPlace
import com.withpet.car.domain.model.PetPlaceCategory
import com.withpet.car.domain.model.RouteInfo
import com.withpet.car.domain.repository.PetPlaceRepository
import com.withpet.car.domain.usecase.DrivingRestrictionMonitor
import com.withpet.car.domain.usecase.GetNearbyPetPlacesUseCase
import com.withpet.car.platform.location.LocationProvider
import com.withpet.car.platform.navigation.NavigationController
import com.withpet.car.ui.place.PlaceViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val navigation = mockk<NavigationController> {
        every { activeRoute() } returns flowOf<RouteInfo?>(null)
        coEvery { searchPlace(any()) } returns Result.success(Unit)
    }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = PlaceViewModel(
        mockk<LocationProvider> { every { currentLocation() } returns flowOf(CITY_HALL) },
        mockk<DrivingRestrictionMonitor> { every { isRestricted } returns MutableStateFlow(false) },
        GetNearbyPetPlacesUseCase(object : PetPlaceRepository { override suspend fun getAll() = emptyList<PetPlace>() }, Clock.systemDefaultZone()),
        navigation,
    )

    @Test
    fun `내비에서 더 찾기는 고른 종류의 키워드로 내비 검색을 연다`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.searchInNavigation()
        vm.setCategory(PetPlaceCategory.TRAVEL)
        vm.searchInNavigation()
        advanceUntilIdle()

        coVerify { navigation.searchPlace("애견동반 카페") }
        coVerify { navigation.searchPlace("애견동반 여행지") }
    }
}
