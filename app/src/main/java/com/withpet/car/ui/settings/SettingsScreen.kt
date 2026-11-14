package com.withpet.car.ui.settings

import com.withpet.car.ui.theme.WithPetColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.withpet.car.domain.model.PetModeSettings
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
                var confirmWindow by remember { mutableStateOf(false) }
                SwitchRow("뒷창문 환기", pm.rearWindowEnabled) { v ->
                    if (v) confirmWindow = true else viewModel.updatePetMode { it.copy(rearWindowEnabled = false) }
                }
                if (confirmWindow) {
                    RearWindowConfirmDialog(
                        onConfirm = {
                            confirmWindow = false
                            viewModel.updatePetMode { it.copy(rearWindowEnabled = true) }
                        },
                        onDismiss = { confirmWindow = false },
                    )
                }
                val minWindow = PetModeSettings.REAR_WINDOW_MIN_PERCENT.toFloat()
                val maxWindow = PetModeSettings.REAR_WINDOW_MAX_PERCENT.toFloat()
                SliderRow(
                    "뒷창문 개방",
                    pm.rearWindowOpenPercent.toFloat().coerceIn(minWindow, maxWindow),
                    minWindow..maxWindow,
                    steps = (maxWindow - minWindow).toInt() - 1,
                    format = { "${it.roundToInt()}%" },
                    enabled = pm.rearWindowEnabled,
                ) { v ->
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
    enabled: Boolean = true,
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
            enabled = enabled,
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

/** 뒷창문 환기를 켤 때 한 번 확인한다 (명세 7-6). */
@Composable
private fun RearWindowConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("뒷창문 환기를 켤까요?", style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                "펫 모드를 켜면 뒷창문이 조금 열려요. 반려동물이 창밖으로 몸을 내밀거나 창틈에 끼이지 않도록 이동장이나 전용 안전벨트를 사용해 주세요. 비가 오면 펫 모드를 켤 때 창문을 열지 않지만, 달리는 중에 비가 오기 시작하면 직접 닫아 주세요. 고속도로에서는 꺼 두는 것이 좋아요.",
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = WithPetColors.Accent), modifier = Modifier.size(width = 160.dp, height = 76.dp)) { Text("켜기", style = MaterialTheme.typography.titleMedium) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = WithPetColors.Accent), modifier = Modifier.size(width = 160.dp, height = 76.dp)) { Text("취소", style = MaterialTheme.typography.titleMedium) }
        },
    )
}
