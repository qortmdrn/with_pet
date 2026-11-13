package com.withpet.car.ui.walk

import com.withpet.car.ui.theme.WithPetColors
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Schedule
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
import com.withpet.car.domain.model.WalkSpotType
import com.withpet.car.domain.usecase.GetNearbyWalkSpotsUseCase
import com.withpet.car.domain.usecase.NearbyWalkSpot
import com.withpet.car.domain.usecase.WalkSpotFilter
import com.withpet.car.ui.components.BigActionButton
import com.withpet.car.ui.components.DrivingAwareList
import com.withpet.car.ui.components.SectionCard
import com.withpet.car.ui.components.StatusBadge
import com.withpet.car.ui.components.TestTags

@Composable
fun WalkScreen(viewModel: WalkViewModel = hiltViewModel()) {
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
                WalkSpotFilter.entries.forEach { f ->
                    FilterChip(
                        selected = state.filter == f,
                        onClick = { viewModel.setFilter(f) },
                        label = { Text(f.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 12.dp)) },
                    )
                }
            }
        }
        state.message?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = WithPetColors.Accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val spots = state.spots
        when {
            spots == null -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            spots.isEmpty() -> Text("근처 ${GetNearbyWalkSpotsUseCase.NEARBY_RADIUS_KM.toInt()}km 안에 지금 산책할 수 있는 곳을 찾지 못했어요", style = MaterialTheme.typography.titleMedium)
            else -> DrivingAwareList(spots, state.restricted) { item ->
                WalkSpotItem(item, hasRoute = state.route != null, restricted = state.restricted, onGo = { viewModel.go(item) })
            }
        }
    }
}

@Composable
private fun WalkSpotItem(item: NearbyWalkSpot, hasRoute: Boolean, restricted: Boolean, onGo: () -> Unit) {
    val spot = item.spot
    SectionCard(Modifier.testTag(TestTags.WALK_ITEM)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(spot.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("%.1fkm".format(item.distanceKm), style = MaterialTheme.typography.bodyLarge)
                    StatusBadge(spot.type.label, WithPetColors.Accent)
                    // 주행 중에는 텍스트 한 줄 이하: 부가 정보는 정차 시에만
                    if (!restricted && spot.largeDogAllowed) StatusBadge("대형견 가능", WithPetColors.Accent)
                }
                if (!restricted) Details(item)
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
private fun Details(item: NearbyWalkSpot) {
    val spot = item.spot
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (spot.leashRequired) Info(Icons.Filled.Link, "목줄 필수")
        Info(Icons.Filled.Payments, spot.fee?.let { "입장료 $it" } ?: "무료")
    }
    // 상시 개방이 아니면 운영시간을 보여 준다
    val hours = listOfNotNull(spot.hoursText, spot.closedDaysText?.takeIf { it != "연중무휴" }?.let { "휴무 $it" }).joinToString(" · ")
    if (hours.isNotEmpty() && spot.hoursText != "매일 00:00~24:00") Info(Icons.Filled.Schedule, hours)
    val pet = listOfNotNull(spot.sizeLimit?.takeIf { !spot.largeDogAllowed }, spot.restriction).joinToString(" · ")
    if (pet.isNotEmpty()) Info(Icons.Filled.Pets, pet)
    Info(Icons.Filled.Home, spot.address)
}

@Composable
private fun Info(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private val WalkSpotFilter.label: String
    get() = when (this) {
        WalkSpotFilter.ALL -> "전체"
        WalkSpotFilter.DOG_PARK -> "반려견 놀이터"
        WalkSpotFilter.PARK -> "공원"
        WalkSpotFilter.TRAIL -> "산책로"
        WalkSpotFilter.LARGE_DOG -> "대형견 가능"
    }

val WalkSpotType.label: String
    get() = when (this) {
        WalkSpotType.DOG_PARK -> "반려견 놀이터"
        WalkSpotType.PARK -> "공원"
        WalkSpotType.TRAIL -> "산책로"
        WalkSpotType.WATERSIDE -> "물가"
        WalkSpotType.REST_AREA -> "쉼터"
    }
