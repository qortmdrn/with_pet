package com.withpet.car.ui.calm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.withpet.car.domain.model.CalmPlaybackState
import com.withpet.car.domain.model.CalmPlaylist
import com.withpet.car.domain.model.CalmSituation
import com.withpet.car.domain.repository.CalmSoundPlayer
import com.withpet.car.domain.repository.PlaylistRepository
import com.withpet.car.domain.usecase.DrivingRestrictionMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CalmUiState(
    val playlists: List<CalmPlaylist> = emptyList(),
    val selected: CalmSituation = CalmSituation.DEPARTURE,
    val playback: CalmPlaybackState = CalmPlaybackState(),
    val restricted: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CalmViewModel @Inject constructor(
    playlistRepository: PlaylistRepository,
    restriction: DrivingRestrictionMonitor,
    private val player: CalmSoundPlayer,
) : ViewModel() {

    private val selected = MutableStateFlow(player.state.value.situation ?: CalmSituation.DEPARTURE)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<CalmUiState> = combine(
        flow { emit(playlistRepository.getAll()) }.onStart { emit(emptyList()) },
        selected,
        player.state,
        restriction.isRestricted,
        error,
    ) { playlists, selected, playback, restricted, error ->
        CalmUiState(playlists, selected, playback, restricted, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalmUiState())

    /** 상황 버튼: 선택한 상황의 플레이리스트를 바로 재생한다. */
    fun play(situation: CalmSituation) {
        selected.value = situation
        viewModelScope.launch {
            error.value = player.play(situation).exceptionOrNull()?.let { "재생하지 못했어요" }
        }
    }

    fun togglePlayStop() {
        if (player.state.value.isPlaying) {
            viewModelScope.launch { player.stop() }
        } else {
            play(selected.value)
        }
    }
}
