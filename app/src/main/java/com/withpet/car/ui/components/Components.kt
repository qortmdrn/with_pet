package com.withpet.car.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.withpet.car.ui.theme.WithPetColors

/** 모든 화면 상단에 고정되는 응급 버튼. 최소 터치 영역 76dp. */
@Composable
fun EmergencyButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 76.dp)
            .widthIn(min = 76.dp)
            .testTag(TestTags.EMERGENCY_BUTTON),
        colors = ButtonDefaults.buttonColors(
            containerColor = WithPetColors.Emergency,
            contentColor = WithPetColors.OnEmergency,
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Icon(Icons.Filled.LocalHospital, contentDescription = null, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        Text("응급", style = MaterialTheme.typography.titleMedium)
    }
}

/** 큰 터치 영역의 주요 동작 버튼 */
@Composable
fun BigActionButton(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 88.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 색만으로 상태를 구분하지 않도록 항상 텍스트를 함께 쓰는 배지 */
@Composable
fun StatusBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.18f),
        contentColor = color,
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

/** 주행 중에는 앞의 [maxWhileDriving]개만 보여 주고 "정차 후 더 보기"를 표시한다. */
@Composable
fun <T> DrivingAwareList(
    items: List<T>,
    restricted: Boolean,
    modifier: Modifier = Modifier,
    maxWhileDriving: Int = MAX_ITEMS_WHILE_DRIVING,
    itemContent: @Composable (T) -> Unit,
) {
    val visible = if (restricted) items.take(maxWhileDriving) else items
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        visible.forEach { itemContent(it) }
        if (restricted && items.size > maxWhileDriving) {
            Text(
                "정차 후 더 보기",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.MORE_AFTER_PARKING),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

const val MAX_ITEMS_WHILE_DRIVING = 3

/** 텍스트 입력이 있는 화면은 주행 중 진입하지 못하게 막는다. */
@Composable
fun ParkedOnly(restricted: Boolean, content: @Composable () -> Unit) {
    if (!restricted) {
        content()
        return
    }
    Box(Modifier.fillMaxSize().testTag(TestTags.PARKED_ONLY), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.PauseCircle, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(16.dp))
            Text("정차 후 이용해 주세요", style = MaterialTheme.typography.titleLarge)
        }
    }
}

object TestTags {
    const val EMERGENCY_BUTTON = "emergency_button"
    const val EMERGENCY_ITEM = "emergency_item"
    const val EMERGENCY_RESULT = "emergency_result"
    const val MORE_AFTER_PARKING = "more_after_parking"
    const val PARKED_ONLY = "parked_only"
    const val WALK_ITEM = "walk_item"
    const val PET_MODE_TOGGLE = "pet_mode_toggle"
}
