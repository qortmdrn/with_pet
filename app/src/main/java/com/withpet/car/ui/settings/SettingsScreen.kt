package com.withpet.car.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.withpet.car.domain.model.RestAlertSettings
import com.withpet.car.ui.components.BigActionButton
import com.withpet.car.ui.components.ParkedOnly
import com.withpet.car.ui.components.SectionCard
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onOpenProfile: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ParkedOnly(state.restricted) {
        if (!state.loaded) return@ParkedOnly
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BigActionButton("반려동물 프로필", Icons.Filled.Pets, onOpenProfile, Modifier.fillMaxWidth())

            val pm = state.petMode
            SectionCard {
                Text("펫 모드 기본값", style = MaterialTheme.typography.titleLarge)
                SliderRow("실내 온도", pm.targetTempC, 18f..26f, steps = 15, format = { "%.1f℃".format(it) }) { v ->
                    viewModel.updatePetMode { it.copy(targetTempC = (v * 2).roundToInt() / 2f) }
                }
                SliderRow("풍량", pm.fanLevel.toFloat(), 1f..5f, steps = 3, format = { "${it.roundToInt()}단계" }) { v ->
                    viewModel.updatePetMode { it.copy(fanLevel = v.roundToInt()) }
                }
                SliderRow("뒷창문 개방", pm.rearWindowOpenPercent.toFloat(), 0f..30f, steps = 5, format = { "${it.roundToInt()}%" }) { v ->
                    viewModel.updatePetMode { it.copy(rearWindowOpenPercent = v.roundToInt()) }
                }
                SliderRow("안정 사운드 볼륨", pm.mediaVolumePercent.toFloat(), 0f..100f, steps = 19, format = { "${it.roundToInt()}%" }) { v ->
                    viewModel.updatePetMode { it.copy(mediaVolumePercent = v.roundToInt()) }
                }
                SwitchRow("안정 사운드 자동 재생", pm.autoPlayCalmSound) { v ->
                    viewModel.updatePetMode { it.copy(autoPlayCalmSound = v) }
                }
            }

            val rest = state.restAlert
            SectionCard {
                Text("휴식 알림", style = MaterialTheme.typography.titleLarge)
                SwitchRow("휴식 알림 사용", rest.enabled) { v -> viewModel.updateRestAlert { it.copy(enabled = v) } }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("알림 간격", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    RestAlertSettings.INTERVAL_OPTIONS.forEach { minutes ->
                        FilterChip(
                            selected = rest.intervalMinutes == minutes,
                            enabled = rest.enabled,
                            onClick = { viewModel.updateRestAlert { it.copy(intervalMinutes = minutes) } },
                            label = { Text("${minutes}분", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 12.dp)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: (Float) -> String,
    onChangeFinished: (Float) -> Unit,
) {
    var current by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(format(current), style = MaterialTheme.typography.bodyLarge)
        }
        Slider(
            value = current,
            onValueChange = { current = it },
            onValueChangeFinished = { onChangeFinished(current) },
            valueRange = range,
            steps = steps,
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
