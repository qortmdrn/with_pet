package com.withpet.car.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.withpet.car.domain.model.DrivingState
import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.VoiceCommand
import com.withpet.car.platform.location.FakeLocationProvider
import com.withpet.car.ui.components.SectionCard

object DebugMenu {
    const val AVAILABLE = true

    @Composable
    fun Screen() = DebugScreen()
}

private val LOCATION_PRESETS = listOf(
    "서울 시청" to FakeLocationProvider.SEOUL_CITY_HALL,
    "강남역" to GeoPoint(37.4979, 127.0276),
    "부산(병원 없음)" to GeoPoint(35.1796, 129.0756),
    "대전(50km 밖)" to GeoPoint(36.3504, 127.3845),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DebugScreen(viewModel: DebugViewModel = hiltViewModel()) {
    val location by viewModel.location.collectAsStateWithLifecycle()
    val driving by viewModel.drivingState.collectAsStateWithLifecycle()
    val minutes by viewModel.drivingMinutes.collectAsStateWithLifecycle()
    val phone by viewModel.phoneConnected.collectAsStateWithLifecycle()
    val windowSupported by viewModel.windowSupported.collectAsStateWithLifecycle()
    val raining by viewModel.raining.collectAsStateWithLifecycle()
    val route by viewModel.route.collectAsStateWithLifecycle()
    val vehicle by viewModel.vehicleSummary.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard {
            Text("위치", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LOCATION_PRESETS.forEach { (name, point) ->
                    FilterChip(selected = location == point, onClick = { viewModel.setLocation(point) }, label = { Text(name) })
                }
                FilterChip(selected = location == null, onClick = { viewModel.setLocation(null) }, label = { Text("위치 미수신") })
            }
        }
        SectionCard {
            Text("주행 상태", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DrivingState.entries.forEach { s ->
                    FilterChip(selected = driving == s, onClick = { viewModel.setDrivingState(s) }, label = { Text(if (s == DrivingState.DRIVING) "주행 중" else "정차") })
                }
            }
            Text("연속 주행 ${minutes}분", style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(0, 59, 60, 89, 90, 120).forEach { m ->
                    OutlinedButton(onClick = { viewModel.setDrivingMinutes(m) }) { Text("${m}분") }
                }
                OutlinedButton(onClick = { viewModel.setDrivingMinutes(minutes + 30) }) { Text("+30분") }
            }
        }
        SectionCard {
            Text("차량 · 휴대폰", style = MaterialTheme.typography.titleMedium)
            ToggleRow("휴대폰 블루투스 연결", phone, viewModel::setPhoneConnected)
            ToggleRow("창문 제어 지원 차량", windowSupported, viewModel::setWindowSupported)
            ToggleRow("비 오는 중 (와이퍼 작동)", raining, viewModel::setRaining)
            Text(vehicle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionCard {
            Text("내비게이션", style = MaterialTheme.typography.titleMedium)
            Text(route ?: "경로 없음", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = viewModel::startSampleRoute) { Text("샘플 경로(일산 방향)") }
                OutlinedButton(onClick = viewModel::clearRoute) { Text("경로 지우기") }
            }
        }
        SectionCard {
            Text("음성 명령 (가짜)", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "강아지 응급이야" to VoiceCommand.Emergency,
                    "펫 모드 켜줘" to VoiceCommand.PetModeOn,
                    "펫 모드 꺼줘" to VoiceCommand.PetModeOff,
                    "응, 추가해" to VoiceCommand.Confirm,
                    "아니, 나중에" to VoiceCommand.Reject,
                ).forEach { (utterance, command) ->
                    OutlinedButton(onClick = { viewModel.sendVoice(command) }) { Text("“$utterance”") }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
