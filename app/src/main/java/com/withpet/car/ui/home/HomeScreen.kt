package com.withpet.car.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.toggleable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.withpet.car.ui.components.BigActionButton
import com.withpet.car.ui.components.PetPhoto
import com.withpet.car.ui.components.SectionCard
import com.withpet.car.ui.components.TestTags
import com.withpet.car.ui.theme.WithPetColors

@Composable
fun HomeScreen(
    onOpenWalk: () -> Unit,
    onOpenPlace: () -> Unit,
    onOpenCalm: () -> Unit,
    onOpenEmergency: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PetCard(state, onOpenProfile)
        PetModeCard(state, viewModel::setPetMode)

        // 주행 중: 펫 모드 토글 + 산책·동반 장소·사운드. 응급은 상단 고정 버튼으로 대신한다.
        // 주요 버튼 3개 제한(7장)의 예외: 동반 장소를 주행 중에도 열 수 있게 하기로 결정 (7-1).
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BigActionButton("산책 스팟", Icons.Filled.Forest, onOpenWalk, Modifier.weight(1f))
            BigActionButton("동반 장소", Icons.Filled.Pets, onOpenPlace, Modifier.weight(1f))
            BigActionButton("안정 사운드", Icons.Filled.MusicNote, onOpenCalm, Modifier.weight(1f))
        }
        if (!state.restricted) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BigActionButton(
                    "응급",
                    Icons.Filled.LocalHospital,
                    onOpenEmergency,
                    Modifier.weight(1f),
                    containerColor = WithPetColors.Emergency,
                    contentColor = WithPetColors.OnEmergency,
                )
                BigActionButton("설정", Icons.Filled.Settings, onOpenSettings, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PetCard(state: HomeUiState, onOpenProfile: () -> Unit) {
    val pet = state.pet
    val clickable = !state.restricted
    SectionCard(Modifier.then(if (clickable) Modifier.clickable(onClick = onOpenProfile) else Modifier)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PetPhoto(pet?.photoUri, size = 88.dp)
            Spacer(Modifier.width(20.dp))
            when {
                pet != null -> Text(pet.name, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                clickable -> {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("반려동물 등록", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                }
                else -> Text("위드펫", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            }
            if (clickable) Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun PetModeCard(state: HomeUiState, onToggle: (Boolean) -> Unit) {
    SectionCard(
        Modifier
            .testTag(TestTags.PET_MODE_TOGGLE)
            .toggleable(value = state.petMode.isOn, role = Role.Switch, onValueChange = onToggle),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("펫 모드", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(if (state.petMode.isOn) "켜짐" else "꺼짐", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(16.dp))
            Switch(checked = state.petMode.isOn, onCheckedChange = null)
        }
        val line = state.statusLine ?: state.petMode.lastReport
        if (line != null) {
            Text(line, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (state.petMode.isOn && state.statusLine != null && state.petMode.lastReport != null && !state.restricted) {
            Text(state.petMode.lastReport, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
