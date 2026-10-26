package com.withpet.car

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.withpet.car.platform.voice.ExternalActionHandler
import com.withpet.car.ui.root.WithPetRoot
import com.withpet.car.ui.theme.WithPetTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var externalActions: ExternalActionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 화면 회전 등으로 다시 만들어질 때 같은 명령을 두 번 처리하지 않는다
        if (savedInstanceState == null) externalActions.handle(intent)
        setContent {
            WithPetTheme {
                WithPetRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalActions.handle(intent)
    }
}
