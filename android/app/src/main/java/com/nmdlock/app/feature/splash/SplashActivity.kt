package com.nmdlock.app.feature.splash

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nmdlock.app.MainActivity
import com.nmdlock.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

/**
 * Splash screen activity.
 * Hien thi splash, kiem tra & xin quyen Shizuku + overlay, sau do vao MainActivity.
 */
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val requestOverlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> proceedToMain() }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        val statusText = findViewById<TextView>(R.id.splash_status_text)
        statusText.text = getString(R.string.splash_loading)

        // Check and request permissions on splash
        scope.launch {
            checkAndRequestPermissions()
            delay(800)
            proceedToMain()
        }
    }

    private suspend fun checkAndRequestPermissions() {
        // 1. Shizuku permission (if available)
        try {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val isRunning = shizukuClass.getMethod("isRunning").invoke(null) as? Boolean ?: false
            val hasPermission = shizukuClass.getMethod("checkSelfPermission").invoke(null) as? Int ?: -1

            if (isRunning && hasPermission != 0) {
                // Request Shizuku permission
                shizukuClass.getMethod("requestPermission", Int::class.javaPrimitiveType)
                    .invoke(null, 1001)
            }
        } catch (_: Exception) {
            // Shizuku not installed - that's ok
        }

        // 2. SYSTEM_ALERT_WINDOW permission for crosshair overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                requestOverlayPermissionLauncher.launch(intent)
            }
        }

        // 3. Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                @Suppress("DEPRECATION")
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1002)
            }
        }
    }

    private fun proceedToMain() {
        val intent = Intent(this@SplashActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
