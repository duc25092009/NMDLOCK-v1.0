package com.nmdlock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.nmdlock.app.core.navigation.NMDNavigation
import com.nmdlock.app.core.ui.theme.NMDLockTheme
import com.nmdlock.app.data.local.DataStoreManager
import com.nmdlock.app.feature.onboarding.OnboardingScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            val isDarkTheme by dataStoreManager
                .isDarkTheme
                .collectAsState(initial = true)

            val isOnboardingComplete by dataStoreManager
                .isOnboardingComplete
                .collectAsState(initial = false)

            NMDLockTheme(
                darkTheme = isDarkTheme
            ) {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {

                    val scope = rememberCoroutineScope()

                    Crossfade(
                        targetState = isOnboardingComplete
                    ) { complete ->

                        when {

                            !complete -> {

                                OnboardingScreen(
                                    onComplete = {

                                        scope.launch {
                                            dataStoreManager
                                                .setOnboardingComplete()
                                        }
                                    }
                                )
                            }

                            else -> {
                                NMDNavigation()
                            }
                        }
                    }
                }
            }
        }
    }
}
