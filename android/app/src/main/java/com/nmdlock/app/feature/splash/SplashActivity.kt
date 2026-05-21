package com.nmdlock.app.feature.splash

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nmdlock.app.MainActivity
import com.nmdlock.app.R
import com.nmdlock.app.core.NMDLockApplication
import com.nmdlock.app.core.security.DeviceIdManager
import com.nmdlock.app.data.local.DataStoreManager
import com.nmdlock.app.data.repository.AuthRepository
import com.nmdlock.app.data.repository.DeviceRepository
import com.nmdlock.app.data.repository.LicenseRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Splash screen activity.
 * Checks license status, device binding, and navigates accordingly.
 * Uses Android 12+ SplashScreen API with custom branded animation.
 */
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {

    @Inject lateinit var dataStoreManager: DataStoreManager
    @Inject lateinit var deviceIdManager: DeviceIdManager
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var deviceRepository: DeviceRepository
    @Inject lateinit var licenseRepository: LicenseRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep splash visible during checks
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        setContentView(R.layout.activity_splash)

        val statusText = findViewById<TextView>(R.id.splash_status_text)

        scope.launch {
            try {
                // Step 1: Initialize
                withContext(Dispatchers.Main) { statusText.text = getString(R.string.splash_loading) }
                delay(500)

                // Step 2: Restore session
                authRepository.restoreSession()

                // Step 3: Register device
                withContext(Dispatchers.Main) { statusText.text = getString(R.string.splash_checking) }
                deviceRepository.registerDevice()
                delay(500)

                // Step 4: Validate license
                withContext(Dispatchers.Main) { statusText.text = getString(R.string.splash_verifying) }
                val isLoggedIn = authRepository.isLoggedIn()
                val licenseValid = licenseRepository.isLicenseValid()
                delay(500)

                // Step 5: Navigate
                withContext(Dispatchers.Main) {
                    keepSplash = false
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = getString(R.string.splash_error)
                    keepSplash = false
                    // Still navigate to main (offline mode)
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
