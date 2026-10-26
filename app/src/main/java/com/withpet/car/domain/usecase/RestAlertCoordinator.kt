package com.withpet.car.domain.usecase

import com.withpet.car.di.ApplicationScope
import com.withpet.car.domain.logic.EtaEstimator
import com.withpet.car.domain.logic.RestAlertPolicy
import com.withpet.car.domain.logic.RestSpotSelector
import com.withpet.car.domain.model.OpenStatus
import com.withpet.car.domain.model.RestAlertSettings
import com.withpet.car.domain.model.WalkSpot
import com.withpet.car.domain.repository.SettingsRepository
import com.withpet.car.domain.repository.WalkSpotRepository
import com.withpet.car.platform.driving.DrivingStateProvider
import com.withpet.car.platform.location.LocationProvider
import com.withpet.car.platform.navigation.NavigationController
import com.withpet.car.platform.voice.SpeechOutput
import com.withpet.car.platform.voice.VoiceCommandSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

data class RestSuggestion(
    val spot: WalkSpot,
    val drivingMinutes: Int,
    val etaMinutes: Int,
) {
    val message: String get() = "${drivingMinutes}분 주행했어요. ${etaMinutes}분 뒤 ${spot.name}에서 쉬어 갈까요?"
}

/** 휴식 알림(F3) 백그라운드 로직. 앱 시작 시 [start]로 구독을 시작한다. */
@Singleton
class RestAlertCoordinator @Inject constructor(
    private val driving: DrivingStateProvider,
    private val petModeManager: PetModeManager,
    private val settingsRepository: SettingsRepository,
    private val walkSpotRepository: WalkSpotRepository,
    private val location: LocationProvider,
    private val navigation: NavigationController,
    private val speech: SpeechOutput,
    private val voice: VoiceCommandSource,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val policy = RestAlertPolicy()
    private val mutex = Mutex()
    private val _suggestion = MutableStateFlow<RestSuggestion?>(null)
    val suggestion: StateFlow<RestSuggestion?> = _suggestion.asStateFlow()

    private var latestMinutes = 0
    private var latestSettings = RestAlertSettings()
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            combine(
                driving.continuousDrivingMinutes(),
                petModeManager.state.map { it.isOn }.distinctUntilChanged(),
                settingsRepository.restAlertSettings(),
            ) { minutes, petModeOn, settings -> Triple(minutes, petModeOn, settings) }
                .collect { (minutes, petModeOn, settings) -> evaluate(minutes, petModeOn, settings) }
        }
    }

    private suspend fun evaluate(minutes: Int, petModeOn: Boolean, settings: RestAlertSettings) = mutex.withLock {
        latestMinutes = minutes
        latestSettings = settings
        if (_suggestion.value != null && (!petModeOn || !settings.enabled)) {
            policy.onDismissed()
            _suggestion.value = null
        }
        if (!policy.shouldAlert(minutes, petModeOn, settings)) return@withLock

        val current = withTimeoutOrNull(READ_TIMEOUT_MS) { location.currentLocation().firstOrNull() } ?: return@withLock
        val route = withTimeoutOrNull(READ_TIMEOUT_MS) { navigation.activeRoute().firstOrNull() }
        // 지금 닫혀 있는 곳은 제안하지 않는다 (공원 대부분은 상시 개방)
        val now = LocalDateTime.now(clock)
        val open = walkSpotRepository.getAll().filter { it.statusAt(now) != OpenStatus.CLOSED }
        val spot = RestSpotSelector.select(open, current, route) ?: return@withLock
        val eta = navigation.estimateEtaMinutes(spot.location).getOrElse {
            EtaEstimator.estimateMinutes(current, spot.location)
        }
        val suggestion = RestSuggestion(spot, minutes, eta)
        policy.onShown()
        _suggestion.value = suggestion
        scope.launch {
            if (!speech.isAvailable() || speech.speak(suggestion.message).isFailure) return@launch
            // 다 읽은 뒤에 듣기 시작해야 자기 목소리를 알아듣지 않는다. 그사이 버튼으로 답했으면 듣지 않는다.
            if (_suggestion.value === suggestion) voice.listenForAnswer()
        }
    }

    /** [추가]: 경로가 있으면 경유지로, 없으면 목적지로 안내한다. */
    suspend fun accept(): Result<Unit> = mutex.withLock {
        val suggestion = _suggestion.value ?: return@withLock Result.success(Unit)
        val hasRoute = withTimeoutOrNull(READ_TIMEOUT_MS) { navigation.activeRoute().firstOrNull() } != null
        val result = if (hasRoute) {
            navigation.addWaypoint(suggestion.spot.location, suggestion.spot.name)
        } else {
            navigation.startGuidance(suggestion.spot.location, suggestion.spot.name)
        }
        policy.onAccepted(latestMinutes, latestSettings)
        _suggestion.value = null
        result
    }

    /** [나중에]: 30분 뒤 재알림. 한 주행에서 2회 거절하면 더 알리지 않는다. */
    suspend fun reject() = mutex.withLock {
        if (_suggestion.value == null) return@withLock
        policy.onRejected(latestMinutes)
        _suggestion.value = null
    }

    private companion object {
        const val READ_TIMEOUT_MS = 1_000L
    }
}
