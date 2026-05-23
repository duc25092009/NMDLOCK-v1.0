package com.nmdlock.app.core.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

/**
 * REAL FPS counter overlay using Android Choreographer API.
 * Draws real-time FPS on top of all apps via SYSTEM_ALERT_WINDOW.
 */
class FpsOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: FpsView? = null
    private var isVisible = false
    private var choreographer: Choreographer? = null
    private var lastFrameTimeNs = 0L
    private var fps = 0
    private var frameCount = 0
    private var lastSecondNs = 0L

    companion object {
        private const val CHANNEL_ID = "fps_overlay"
        private const val NOTIFICATION_ID = 1002
        private var running = false
        private var instance: FpsOverlayService? = null

        var fpsTextSize = 28f
        var fpsColor = Color.GREEN
        var fpsOpacity = 200
        var showMinFps = true
        var showAvgFps = true

        private var currentFps = 0
        private var minFps = 999
        private var avgFps = 0
        private var frameHistory = mutableListOf<Int>()

        fun getCurrentFps(): Int = currentFps
        fun getMinFps(): Int = if (minFps == 999) 0 else minFps
        fun getAvgFps(): Int = avgFps

        fun isRunning(): Boolean = running

        fun start(context: Context) {
            if (!running) {
                val intent = Intent(context, FpsOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            if (running) {
                context.stopService(Intent(context, FpsOverlayService::class.java))
                resetStats()
            }
        }

        private fun resetStats() {
            currentFps = 0
            minFps = 999
            avgFps = 0
            frameHistory.clear()
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Notification channel for foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "FPS Counter",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "FPS counter overlay"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NMDLock")
            .setContentText("FPS Counter đang hoạt động")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        choreographer = Choreographer.getInstance()
        running = true
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showOverlay()
        startFrameCallback()
        return START_STICKY
    }

    override fun onDestroy() {
        stopFrameCallback()
        hideOverlay()
        running = false
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        if (isVisible) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 50
        params.y = 50

        overlayView = FpsView(this)
        windowManager.addView(overlayView!!, params)
        isVisible = true
    }

    private fun hideOverlay() {
        if (isVisible && overlayView != null) {
            try { windowManager.removeView(overlayView) } catch (_: Exception) { }
            overlayView = null
            isVisible = false
        }
    }

    private fun startFrameCallback() {
        choreographer?.postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                val nowNs = System.nanoTime()

                if (lastSecondNs == 0L) {
                    lastSecondNs = nowNs
                    lastFrameTimeNs = frameTimeNanos
                }

                frameCount++
                val elapsedNs = nowNs - lastSecondNs

                if (elapsedNs >= 1_000_000_000L) {
                    currentFps = frameCount
                    frameHistory.add(frameCount)

                    // Min/Avg tracking
                    if (frameCount < minFps) minFps = frameCount
                    avgFps = frameHistory.average().toInt()
                    if (frameHistory.size > 300) frameHistory.removeAt(0) // Keep 5 min history

                    frameCount = 0
                    lastSecondNs = nowNs

                    overlayView?.invalidate()
                }

                choreographer?.postFrameCallback(this)
            }
        })
    }

    private fun stopFrameCallback() {
        choreographer?.let { ch ->
            // Remove callbacks by posting a dummy one
            ch.postFrameCallback { }
        }
    }

    inner class FpsView(context: Context) : View(context) {
        private val paint = Paint().apply {
            isAntiAlias = true
            isFakeBoldText = true
        }
        private val bgPaint = Paint().apply {
            color = Color.argb(100, 0, 0, 0)
            style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val displayFps = currentFps
            val displayMin = getMinFps()
            val displayAvg = getAvgFps()

            val color = when {
                displayFps >= 55 -> Color.GREEN
                displayFps >= 40 -> Color.YELLOW
                else -> Color.RED
            }

            paint.color = color
            paint.alpha = fpsOpacity
            paint.textSize = fpsTextSize

            // Background
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 12f, 12f, bgPaint)

            // FPS text
            val fpsText = "$displayFps FPS"
            canvas.drawText(fpsText, 12f, fpsTextSize + 4f, paint)

            var yOffset = fpsTextSize + 16f
            paint.textSize = fpsTextSize * 0.6f
            paint.color = color.copy(alpha = 180)

            if (showMinFps) {
                canvas.drawText("Min: $displayMin", 12f, yOffset, paint)
                yOffset += fpsTextSize * 0.7f
            }
            if (showAvgFps) {
                canvas.drawText("Avg: $displayAvg", 12f, yOffset, paint)
            }
        }
    }
}
