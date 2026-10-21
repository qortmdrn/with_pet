package com.withpet.car

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.withpet.car.ui.root.WithPetRoot
import com.withpet.car.ui.theme.WithPetTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WithPetTheme {
                WithPetRoot()
            }
        }
    }
}
