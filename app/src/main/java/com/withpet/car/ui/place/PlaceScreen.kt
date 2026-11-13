package com.withpet.car.ui.place

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.withpet.car.domain.model.OpenStatus
import com.withpet.car.domain.model.PetPlace
import com.withpet.car.domain.model.PetPlaceCategory
import com.withpet.car.domain.usecase.GetNearbyPetPlacesUseCase
import com.withpet.car.domain.usecase.NearbyPetPlace
import com.withpet.car.ui.components.BigActionButton
import com.withpet.car.ui.components.DrivingAwareList
import com.withpet.car.ui.components.SectionCard
import com.withpet.car.ui.components.StatusBadge
import com.withpet.car.ui.components.TestTags
import com.withpet.car.ui.theme.WithPetColors

// 주행 중: 종류 칩·내비 검색 버튼을 숨기고 고른 종류의 가까운 3곳만 (주요 버튼 3개·텍스트 한 줄 제한).
@Composable
fun PlaceScreen(viewModel: PlaceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!state.restricted) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PetPlaceCategory.entries.forEach { c ->
                    FilterChip(
                        selected = state.category == c,
                        onClick = { viewModel.setCategory(c) },
                        label = { Text(c.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 12.dp)) },
                    )
                }
            }
        }
        state.message?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = WithPetColors.Accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val result = state.result
        when {
            result == null -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            result.places.isEmpty() -> Text("근처 ${GetNearbyPetPlacesUseCase.EXPANDED_RADIUS_KM.toInt()}km 안에 동반 가능한 ${state.category.label}을 찾지 못했어요", style = MaterialTheme.typography.titleMedium)
            else -> {
                if (result.searchedRadiusKm > GetNearbyPetPlacesUseCase.PRIMARY_RADIUS_KM && !state.restricted) {
                    Text("가까운 곳이 없어 ${result.searchedRadiusKm.toInt()}km까지 찾았어요", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DrivingAwareList(result.places, state.restricted) { item ->
                    PlaceItem(item, hasRoute = state.route != null, restricted = state.restricted, onGo = { viewModel.go(item) })
                }
            }
        }
        if (!state.restricted) {
            BigActionButton(
                "내비에서 더 찾기",
                Icons.Filled.Search,
                viewModel::searchInNavigation,
                Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.PLACE_NAVI_SEARCH),
            )
            Text(
                "출처: 반려동물 동반 가능 문화시설 공공데이터. 운영 정보가 바뀌었을 수 있으니 방문 전 확인해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlaceItem(item: NearbyPetPlace, hasRoute: Boolean, restricted: Boolean, onGo: () -> Unit) {
    val place = item.place
    SectionCard(Modifier.testTag(TestTags.PLACE_ITEM)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(place.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("%.1fkm".format(item.distanceKm), style = MaterialTheme.typography.bodyLarge)
                    OpenStatusBadge(item.status)
                    // 주행 중에는 텍스트 한 줄 이하: 부가 정보는 정차 시에만
                    if (!restricted && place.largeDogAllowed) StatusBadge("대형견 가능", WithPetColors.Accent)
                }
                if (!restricted) Details(place)
            }
            BigActionButton(
                if (hasRoute) "경유지로 추가" else "길 안내",
                if (hasRoute) Icons.Filled.AddLocationAlt else Icons.Filled.Directions,
                onGo,
                Modifier.heightIn(min = 88.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun Details(place: PetPlace) {
    val hours = listOfNotNull(place.hoursText, place.closedDaysText?.let { "휴무 $it" }).joinToString(" · ")
    if (hours.isNotEmpty()) Info(Icons.Filled.Schedule, hours)
    val pet = listOfNotNull(
        place.sizeLimit?.takeIf { !place.largeDogAllowed },
        place.restriction,
        place.extraFee?.let { "추가 요금 $it" },
    ).joinToString(" · ")
    if (pet.isNotEmpty()) Info(Icons.Filled.Pets, pet)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f, fill = false)) { Info(Icons.Filled.Home, place.address) }
        if (place.hasParking) Info(Icons.Filled.LocalParking, "주차")
    }
}

@Composable
private fun Info(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun OpenStatusBadge(status: OpenStatus) = when (status) {
    OpenStatus.OPEN -> StatusBadge("영업 중", WithPetColors.Open)
    OpenStatus.UNKNOWN -> StatusBadge("영업시간 확인 필요", WithPetColors.Unknown)
    OpenStatus.CLOSED -> StatusBadge("영업 종료", WithPetColors.Closed)
}

val PetPlaceCategory.label: String
    get() = when (this) {
        PetPlaceCategory.CAFE -> "카페·식당"
        PetPlaceCategory.TRAVEL -> "여행지"
        PetPlaceCategory.CULTURE -> "문화시설"
    }
