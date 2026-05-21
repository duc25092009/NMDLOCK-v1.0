package com.nmdlock.app.feature.splash
 
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nmdlock.app.MainActivity
import com.nmdlock.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
 
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {
 
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
 
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
 
        setContentView(R.layout.activity_splash)
 
        val statusText = findViewById<TextView>(R.id.splash_status_text)
        statusText.text = getString(R.string.splash_loading)
 
        scope.launch {
            delay(1500)
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
 
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
