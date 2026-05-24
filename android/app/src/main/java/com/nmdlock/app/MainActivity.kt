package com.nmdlock.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.nmdlock.app.core.navigation.NMDNavigation
import com.nmdlock.app.core.ui.theme.NMDLockTheme
import com.nmdlock.app.data.local.DataStoreManager
import com.nmdlock.app.feature.license.LicenseGateScreen
import com.nmdlock.app.feature.onboarding.OnboardingScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main entry point for the NMDLock app after splash.
 * Kiểm tra onboarding + license trước khi vào main navigation.
 * 
 * FIX: Dùng sealed class state thay vì multiple boolean + Crossfade
 * để tránh lỗi content biến mất khi state thay đổi.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    // Sealed class cho state management rõ ràng, tránh race condition
    private sealed class AppState {
        object Loading : AppState()
        object Onboarding : AppState()
        object LicenseCheck : AppState()
        object Ready : AppState() // Đã qua license, vào app chính
        data class Error(val message: String) : AppState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        Log.d("NMD_MAIN", "=== MainActivity onCreate ===")

        setContent {
            val scope = rememberCoroutineScope()
            
            // Collect tất cả state cần thiết
            val isDarkTheme by dataStoreManager.isDarkTheme.collectAsState(initial = true)
            
            // State chính của app - dùng remember để giữ state giữa recompositions
            var appState by remember { mutableStateOf<AppState>(AppState.Loading) }
            
            // Init state khi Compose first render
            LaunchedEffect(Unit) {
                Log.d("NMD_MAIN", "LaunchedEffect: Checking app state...")
                
                try {
                    // Đọc onboarding status từ DataStore (blocking call trong coroutine)
                    val onboardingDone = dataStoreManager.isOnboardingComplete.first()
                    Log.d("NMD_MAIN", "Onboarding complete: $onboardingDone")
                    
                    appState = if (!onboardingDone) {
                        AppState.Onboarding
                    } else {
                        // Đã qua onboarding → check license
                        AppState.LicenseCheck
                    }
                } catch (e: Exception) {
                    Log.e("NMD_MAIN", "Error reading onboarding state", e)
                    appState = AppState.Error("Init error: ${e.message}")
                }
            }

            NMDLockTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Log.d("NMD_MAIN", "Rendering AppState: $appState")
                    
                    when (val state = appState) {
                        is AppState.Loading -> {
                            Log.d("NMD_MAIN", "Showing Loading screen")
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                        
                        is AppState.Onboarding -> {
                            Log.d("NMD_MAIN", "Showing OnboardingScreen")
                            OnboardingScreen(
                                onComplete = {
                                    Log.d("NMD_MAIN", "Onboarding completed")
                                    scope.launch {
                                        dataStoreManager.setOnboardingComplete()
                                        appState = AppState.LicenseCheck
                                    }
                                }
                            )
                        }
                        
                        is AppState.LicenseCheck -> {
                            Log.d("NMD_MAIN", "Showing LicenseGateScreen")
                            LicenseGateScreen(
                                onLicenseValid = {
                                    Log.d("NMD_MAIN", "License validated! Entering app")
                                    // FIX QUAN TRỌNG: Gọi setState để trigger re-composition
                                    appState = AppState.Ready
                                },
                                onLicenseError = { error ->
                                    Log.e("NMD_MAIN", "License error: $error")
                                    // Vẫn cho vào app nhưng có thể show warning
                                    // Hoặc giữ ở màn license tùy policy
                                    appState = AppState.Ready
                                }
                            )
                        }
                        
                        is AppState.Ready -> {
                            Log.d("NMD_MAIN", "Showing NMDNavigation (MAIN APP) ✓✓✓")
                            // FIX QUAN TRỌNG: Đảm bảo NMDNavigation được render đúng
                            // và không bị unmount do state change
                            NMDNavigation(
                                onLicenseExpired = {
                                    Log.w("NMD_MAIN", "License expired during usage")
                                    appState = AppState.LicenseCheck
                                }
                            )
                        }
                        
                        is AppState.Error -> {
                            Log.e("NMD_MAIN", "Showing error: ${state.message}")
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.Text(
                                    text = "⚠️ Lỗi: ${state.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("NMD_MAIN", "=== MainActivity onResume ===")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d("NMD_MAIN", "=== MainActivity onPause ===")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d("NMD_MAIN", "=== MainActivity onDestroy ===")
    }
}
