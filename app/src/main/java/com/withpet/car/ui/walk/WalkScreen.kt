package com.withpet.car.ui.walk

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.WaterDrop
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
            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val spots = state.spots
        when {
            spots == null -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            spots.isEmpty() -> Text("근처에 산책할 곳을 찾지 못했어요", style = MaterialTheme.typography.titleMedium)
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
                    StatusBadge(spot.type.label, MaterialTheme.colorScheme.primary)
                    // 주행 중에는 텍스트 한 줄 이하: 부가 정보는 정차 시에만
                    if (!restricted && spot.largeDogAllowed) StatusBadge("대형견 가능", MaterialTheme.colorScheme.primary)
                }
                if (!restricted) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (spot.leashRequired) Amenity(Icons.Filled.Link, "목줄 필수")
                        if (spot.hasWasteBin) Amenity(Icons.Filled.Delete, "배변봉투함")
                        if (spot.hasWater) Amenity(Icons.Filled.WaterDrop, "음수대")
                    }
                }
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
private fun Amenity(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        WalkSpotType.OPEN_FIELD -> "공터"
        WalkSpotType.REST_AREA -> "쉼터"
    }
