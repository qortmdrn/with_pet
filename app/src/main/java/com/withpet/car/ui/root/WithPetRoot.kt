package com.withpet.car.ui.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.withpet.car.debug.DebugMenu
import com.withpet.car.domain.usecase.RestSuggestion
import com.withpet.car.ui.calm.CalmScreen
import com.withpet.car.ui.components.BigActionButton
import com.withpet.car.ui.components.EmergencyButton
import com.withpet.car.ui.emergency.EmergencyScreen
import com.withpet.car.ui.home.HomeScreen
import com.withpet.car.ui.place.PlaceScreen
import com.withpet.car.ui.profile.ProfileEditScreen
import com.withpet.car.ui.profile.ProfileEditViewModel
import com.withpet.car.ui.profile.ProfileListScreen
import com.withpet.car.ui.settings.SettingsScreen
import com.withpet.car.ui.walk.WalkScreen
import com.withpet.car.ui.theme.WithPetColors

object Routes {
    const val HOME = "home"
    const val EMERGENCY = "emergency"
    const val WALK = "walk"
    const val PLACE = "place"
    const val CALM = "calm"
    const val PROFILE = "profile"
    const val PROFILE_EDIT = "profile/edit/{${ProfileEditViewModel.ARG_PET_ID}}"
    const val SETTINGS = "settings"
    const val DEBUG = "debug"

    fun profileEdit(id: Long) = "profile/edit/$id"

    fun title(route: String?): String = when (route) {
        EMERGENCY -> "응급 · 동물병원 찾기"
        WALK -> "산책 스팟"
        PLACE -> "반려동물 동반 장소"
        CALM -> "안정 사운드"
        PROFILE -> "반려동물 프로필"
        PROFILE_EDIT -> "프로필 편집"
        SETTINGS -> "설정"
        DEBUG -> "디버그 메뉴"
        else -> "위드펫"
    }
}

@Composable
fun WithPetRoot(viewModel: AppViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val restricted by viewModel.restricted.collectAsStateWithLifecycle()
    val petMode by viewModel.petMode.collectAsStateWithLifecycle()
    val restSuggestion by viewModel.restSuggestion.collectAsStateWithLifecycle()

    val openEmergency = { navController.navigate(Routes.EMERGENCY) { launchSingleTop = true } }
    LaunchedEffect(Unit) { viewModel.openEmergency.collect { openEmergency() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopBar(
                title = Routes.title(route),
                canGoBack = route != null && route != Routes.HOME,
                onBack = { navController.popBackStack() },
                onEmergency = openEmergency,
                onDebug = if (DebugMenu.AVAILABLE) {
                    { navController.navigate(Routes.DEBUG) { launchSingleTop = true } }
                } else null,
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AppNavHost(navController, restricted)

            // 응급 화면에는 팝업을 일절 띄우지 않는다. 나머지 화면으로 돌아오면 표시한다.
            if (route != Routes.EMERGENCY) {
                restSuggestion?.let {
                    RestAlertCard(
                        it,
                        onAccept = viewModel::acceptRest,
                        onLater = viewModel::rejectRest,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
                if (petMode.pendingSafetyCheck) SafetyCheckDialog(viewModel::acknowledgeSafetyCheck)
            }
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, restricted: Boolean) {
    NavHost(navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenWalk = { navController.navigate(Routes.WALK) },
                onOpenPlace = { navController.navigate(Routes.PLACE) },
                onOpenCalm = { navController.navigate(Routes.CALM) },
                onOpenEmergency = { navController.navigate(Routes.EMERGENCY) { launchSingleTop = true } },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.EMERGENCY) { EmergencyScreen() }
        composable(Routes.WALK) { WalkScreen() }
        composable(Routes.PLACE) { PlaceScreen() }
        composable(Routes.CALM) { CalmScreen() }
        composable(Routes.PROFILE) {
            ProfileListScreen(
                onEdit = { navController.navigate(Routes.profileEdit(it)) },
                onAdd = { navController.navigate(Routes.profileEdit(0)) },
            )
        }
        composable(
            Routes.PROFILE_EDIT,
            arguments = listOf(navArgument(ProfileEditViewModel.ARG_PET_ID) { type = NavType.LongType }),
        ) {
            ProfileEditScreen(restricted = restricted, onDone = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) { SettingsScreen(onOpenProfile = { navController.navigate(Routes.PROFILE) }) }
        composable(Routes.DEBUG) { DebugMenu.Screen() }
    }
}

@Composable
private fun TopBar(
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onEmergency: () -> Unit,
    onDebug: (() -> Unit)?,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (canGoBack) {
                IconButton(onClick = onBack, modifier = Modifier.size(76.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", modifier = Modifier.size(40.dp))
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            if (onDebug != null) {
                IconButton(onClick = onDebug, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Filled.BugReport, contentDescription = "디버그 메뉴")
                }
                Spacer(Modifier.width(8.dp))
            }
            EmergencyButton(onClick = onEmergency)
        }
    }
}

/** 휴식 알림 오버레이 카드 (명세 7-4) */
@Composable
private fun RestAlertCard(
    suggestion: RestSuggestion,
    onAccept: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(36.dp), tint = WithPetColors.Primary)
                Spacer(Modifier.width(12.dp))
                Text(suggestion.message, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BigActionButton(
                    "추가",
                    Icons.Filled.AddLocationAlt,
                    onAccept,
                    Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
                BigActionButton("나중에", null, onLater, Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.surface)
            }
        }
    }
}

/** 안전 탑승 체크 (F6) */
@Composable
private fun SafetyCheckDialog(onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("안전 탑승 체크", style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                "반려동물이 이동장 또는 전용 안전벨트에 있나요? 운전석에 안고 운전하는 것은 도로교통법 위반이에요.",
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.size(width = 160.dp, height = 76.dp)) {
                Text("확인", style = MaterialTheme.typography.titleMedium)
            }
        },
    )
}
