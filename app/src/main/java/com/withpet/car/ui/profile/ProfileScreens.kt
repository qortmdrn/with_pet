package com.withpet.car.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.withpet.car.domain.model.Species
import com.withpet.car.ui.components.BigActionButton
import com.withpet.car.ui.components.ParkedOnly
import com.withpet.car.ui.components.PetPhoto
import com.withpet.car.ui.components.SectionCard
import com.withpet.car.ui.components.StatusBadge
import com.withpet.car.ui.theme.WithPetColors

@Composable
fun ProfileListScreen(
    onEdit: (Long) -> Unit,
    onAdd: () -> Unit,
    viewModel: ProfileListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ParkedOnly(state.restricted) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.pets.isEmpty()) {
                Text("등록된 반려동물이 없어요", style = MaterialTheme.typography.titleMedium)
            }
            state.pets.forEach { pet ->
                val isRep = pet.id == state.representativeId
                SectionCard(Modifier.clickable { onEdit(pet.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PetPhoto(pet.photoUri, size = 72.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(pet.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (isRep) {
                                    Spacer(Modifier.width(12.dp))
                                    StatusBadge("대표", WithPetColors.Primary)
                                }
                            }
                            Text(pet.summary(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { viewModel.setRepresentative(pet.id) }) {
                            Icon(if (isRep) Icons.Filled.Star else Icons.Filled.StarBorder, contentDescription = "대표로 지정")
                        }
                        Icon(Icons.Filled.Edit, contentDescription = "수정")
                    }
                }
            }
            BigActionButton("반려동물 추가", Icons.Filled.Add, onAdd, Modifier.fillMaxWidth())
        }
    }
}

private fun com.withpet.car.domain.model.PetProfile.summary(): String = listOfNotNull(
    species.label,
    breed,
    birthYear?.let { "${it}년생" },
    weightKg?.let { "${it}kg" },
).joinToString(" · ")

@Composable
fun ProfileEditScreen(
    restricted: Boolean,
    onDone: () -> Unit,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setPhoto(uri)
    }
    ParkedOnly(restricted) {
        val form = state.form
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PetPhoto(form.photoUri, size = 120.dp)
                Spacer(Modifier.width(24.dp))
                BigActionButton(
                    "사진 선택",
                    Icons.Filled.PhotoCamera,
                    { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                )
            }
            OutlinedTextField(
                value = form.name,
                onValueChange = { v -> viewModel.edit { it.copy(name = v) } },
                label = { Text("이름") },
                isError = state.showErrors && form.nameError,
                supportingText = { if (state.showErrors && form.nameError) Text("이름을 입력해 주세요") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Species.entries.forEach { s ->
                    FilterChip(
                        selected = form.species == s,
                        onClick = { viewModel.edit { it.copy(species = s) } },
                        label = { Text(s.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 12.dp)) },
                    )
                }
            }
            OutlinedTextField(
                value = form.breed,
                onValueChange = { v -> viewModel.edit { it.copy(breed = v) } },
                label = { Text("품종 (선택)") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = form.birthYear,
                    onValueChange = { v -> viewModel.edit { it.copy(birthYear = v.filter(Char::isDigit).take(4)) } },
                    label = { Text("태어난 해 (선택)") },
                    isError = state.showErrors && form.birthYearError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = form.weightKg,
                    onValueChange = { v -> viewModel.edit { it.copy(weightKg = v.filter { c -> c.isDigit() || c == '.' }.take(5)) } },
                    label = { Text("체중 kg (선택)") },
                    isError = state.showErrors && form.weightError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BigActionButton(
                    "저장",
                    Icons.Filled.Save,
                    viewModel::save,
                    Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
                if (!state.isNew) {
                    BigActionButton("삭제", Icons.Filled.Delete, viewModel::delete, Modifier.weight(1f))
                }
            }
        }
    }
}

val Species.label: String
    get() = when (this) {
        Species.DOG -> "강아지"
        Species.CAT -> "고양이"
        Species.OTHER -> "기타"
    }
