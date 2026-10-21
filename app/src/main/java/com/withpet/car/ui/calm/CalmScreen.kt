package com.withpet.car.ui.calm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.withpet.car.domain.model.CalmSituation
import com.withpet.car.ui.components.BigActionButton
import com.withpet.car.ui.components.SectionCard

// 주행 중 화면: 상황 선택 버튼 4개 + 재생/정지만. 영상은 MVP에서 제외.
@Composable
fun CalmScreen(viewModel: CalmViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CalmSituation.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { situation ->
                    val active = state.playback.isPlaying && state.playback.situation == situation
                    BigActionButton(
                        situation.label,
                        situation.icon,
                        onClick = { viewModel.play(situation) },
                        modifier = Modifier.weight(1f),
                        containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        BigActionButton(
            if (state.playback.isPlaying) "정지" else "재생",
            if (state.playback.isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            viewModel::togglePlayStop,
            Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
        state.error?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) }

        if (!state.restricted) {
            val playback = state.playback
            if (playback.isPlaying && playback.trackTitle != null) {
                Text("재생 중 · ${playback.trackTitle}", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            state.playlists.find { it.situation == state.selected }?.let { playlist ->
                SectionCard {
                    Text(playlist.title, style = MaterialTheme.typography.titleMedium)
                    playlist.tracks.forEach { track ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(track.title, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

private val CalmSituation.label: String
    get() = when (this) {
        CalmSituation.DEPARTURE -> "출발"
        CalmSituation.LONG_DRIVE -> "장거리"
        CalmSituation.TO_HOSPITAL -> "병원 가는 길"
        CalmSituation.RETURN_HOME -> "귀가"
    }

private val CalmSituation.icon
    get() = when (this) {
        CalmSituation.DEPARTURE -> Icons.Filled.DirectionsCar
        CalmSituation.LONG_DRIVE -> Icons.Filled.Route
        CalmSituation.TO_HOSPITAL -> Icons.Filled.LocalHospital
        CalmSituation.RETURN_HOME -> Icons.Filled.Home
    }
