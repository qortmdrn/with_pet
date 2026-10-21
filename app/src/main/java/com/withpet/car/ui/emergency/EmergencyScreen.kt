package com.withpet.car.ui.emergency

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.withpet.car.domain.model.HospitalCandidate
import com.withpet.car.domain.model.OpenStatus
import com.withpet.car.domain.usecase.FindEmergencyHospitalsUseCase
import com.withpet.car.ui.components.BigActionButton
import com.withpet.car.ui.components.SectionCard
import com.withpet.car.ui.components.StatusBadge
import com.withpet.car.ui.components.TestTags
import com.withpet.car.ui.theme.WithPetColors

// 응급 모드 화면에는 광고·추천·팝업을 일절 표시하지 않는다. 원격 진료로 오해될 문구 금지.
// 주행 중에도 전체 기능을 쓸 수 있다 (후보가 최대 3곳이므로 목록 제한과도 맞는다).
@Composable
fun EmergencyScreen(viewModel: EmergencyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    EmergencyContent(state, onSelect = viewModel::select, onRetry = viewModel::refresh, onBackToList = viewModel::clearSelection)
}

@Composable
fun EmergencyContent(
    state: EmergencyUiState,
    onSelect: (HospitalCandidate) -> Unit,
    onRetry: () -> Unit,
    onBackToList: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val selection = state.selection
        if (selection != null) {
            SelectionPanel(selection, onBackToList)
            return@Column
        }
        when (val list = state.list) {
            EmergencyListState.Loading -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            EmergencyListState.NoLocation -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOff, contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("현재 위치를 확인할 수 없어요", style = MaterialTheme.typography.titleMedium)
                }
                BigActionButton("다시 찾기", Icons.Filled.Refresh, onRetry)
            }
            is EmergencyListState.Loaded -> {
                val result = list.result
                if (result.candidates.isNotEmpty()) {
                    Text(
                        if (result.searchedRadiusKm > FindEmergencyHospitalsUseCase.PRIMARY_RADIUS_KM) "가까운 동물병원 (50km 이내)" else "가까운 동물병원",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    result.candidates.forEach { HospitalItem(it, onSelect) }
                } else {
                    Text("근처에 진료 중인 병원을 찾지 못했어요", style = MaterialTheme.typography.titleLarge)
                    result.nearestFallback?.let {
                        Text("가장 가까운 병원", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HospitalItem(it, onSelect)
                    }
                }
            }
        }
    }
}

@Composable
private fun HospitalItem(candidate: HospitalCandidate, onSelect: (HospitalCandidate) -> Unit) {
    val hospital = candidate.hospital
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .testTag(TestTags.EMERGENCY_ITEM)
            .clickable { onSelect(candidate) },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        hospital.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (hospital.is24Hours) StatusBadge("24시", WithPetColors.Primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${candidate.etaMinutes}분", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OpenStatusBadge(candidate.status)
                }
            }
            Spacer(Modifier.width(16.dp))
            Surface(color = WithPetColors.Emergency, contentColor = WithPetColors.OnEmergency, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("병원에 전화하고 길 안내", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun OpenStatusBadge(status: OpenStatus) = when (status) {
    OpenStatus.OPEN -> StatusBadge("진료 중", WithPetColors.Open)
    OpenStatus.UNKNOWN -> StatusBadge("진료 시간 미확인 · 전화 확인 필요", WithPetColors.Unknown)
    OpenStatus.CLOSED -> StatusBadge("진료 종료 · 전화 확인 필요", WithPetColors.Closed)
}

@Composable
private fun SelectionPanel(selection: EmergencySelection, onBackToList: () -> Unit) {
    val hospital = selection.candidate.hospital
    val result = selection.result
    SectionCard(Modifier.testTag(TestTags.EMERGENCY_RESULT)) {
        Text(hospital.name, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        when {
            result == null -> CircularProgressIndicator()
            result.callStarted -> Line(Icons.Filled.Call, "전화를 연결하고 있어요")
            else -> {
                // 휴대폰 미연결: 번호를 크게 보여 준다
                Text(hospital.phone, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = WithPetColors.Primary)
                Text("휴대폰을 연결하면 바로 전화할 수 있어요", style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (result != null) {
            Line(
                Icons.Filled.Directions,
                if (result.guidanceStarted) "길 안내를 시작했어요 · ${selection.candidate.etaMinutes}분" else "길 안내를 시작하지 못했어요",
            )
        }
    }
    BigActionButton("다른 병원 보기", null, onBackToList, Modifier.fillMaxWidth())
}

@Composable
private fun Line(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}
