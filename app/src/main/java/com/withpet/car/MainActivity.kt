package com.withpet.car

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.withpet.car.platform.voice.ExternalActionHandler
import com.withpet.car.ui.root.WithPetRoot
import com.withpet.car.ui.theme.WithPetTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var externalActions: ExternalActionHandler

    // 결과는 따로 처리하지 않는다: 위치 제공자가 권한이 생길 때까지 기다렸다가 구독한다
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 화면 회전 등으로 다시 만들어질 때 같은 명령을 두 번 처리하지 않는다
        if (savedInstanceState == null) {
            externalActions.handle(intent)
            requestRuntimePermissions()
        }
        setContent {
            WithPetTheme {
                WithPetRoot()
            }
        }
    }

    /** 매니페스트에 선언된 런타임 권한만 요청한다 (fake flavor에는 Pleos 권한이 없다). */
    private fun requestRuntimePermissions() {
        val declared = runCatching {
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions
        }.getOrNull()?.toSet().orEmpty()
        val needed = RUNTIME_PERMISSIONS.filter {
            it in declared && checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalActions.handle(intent)
    }

    private companion object {
        val RUNTIME_PERMISSIONS = listOf("pleos.car.permission.FUSED_LOCATION")
    }
}
