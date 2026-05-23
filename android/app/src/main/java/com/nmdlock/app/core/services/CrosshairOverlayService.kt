package com.nmdlock.app.core.services

import android.app.Notification
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
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

/**
 * Draws a REAL crosshair overlay on top of all apps using SYSTEM_ALERT_WINDOW permission.
 * Controlled via companion object static methods.
 */
class CrosshairOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: CrosshairView? = null
    private var isVisible = false

    companion object {
        private const val TAG = "CrosshairOverlay"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "crosshair_overlay"
        private var running = false
        private var instance: CrosshairOverlayService? = null

        // Current crosshair properties
        var crosshairSize = 40f
        var crosshairColor = Color.RED
        var crosshairOpacity = 180
        var crosshairThickness = 2f
        var crosshairGap = 8f
        var dotEnabled = true
        var circleEnabled = true

        fun isOverlayRunning(): Boolean = running

        fun start(context: Context) {
            if (!running) {
                val intent = Intent(context, CrosshairOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            if (running) {
                context.stopService(Intent(context, CrosshairOverlayService::class.java))
            }
        }

        fun updateCrosshair(
            size: Float = crosshairSize,
            color: Int = crosshairColor,
            opacity: Int = crosshairOpacity,
            thickness: Float = crosshairThickness,
            gap: Float = crosshairGap,
            dot: Boolean = dotEnabled,
            circle: Boolean = circleEnabled,
        ) {
            crosshairSize = size
            crosshairColor = color
            crosshairOpacity = opacity
            crosshairThickness = thickness
            crosshairGap = gap
            dotEnabled = dot
            circleEnabled = circle
            instance?.overlayView?.invalidate()
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Create notification channel for foreground service (required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Crosshair Overlay",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Hiển thị thông báo khi crosshair overlay đang chạy"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NMDLock")
            .setContentText("Crosshair overlay đang hoạt động")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        running = true
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        hideOverlay()
        running = false
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        if (isVisible) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.TOP or Gravity.START

        overlayView = CrosshairView(this)
        windowManager.addView(overlayView!!, params)
        isVisible = true
    }

    private fun hideOverlay() {
        if (isVisible && overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (_: Exception) { }
            overlayView = null
            isVisible = false
        }
    }

    inner class CrosshairView(context: Context) : View(context) {
        private val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val centerX = width / 2f
            val centerY = height / 2f

            paint.color = crosshairColor
            paint.alpha = crosshairOpacity
            paint.strokeWidth = crosshairThickness
            paint.style = Paint.Style.STROKE

            val halfSize = crosshairSize / 2f
            val gap = crosshairGap

            // Four cross lines
            canvas.drawLine(centerX, centerY - gap, centerX, centerY - gap - halfSize, paint)
            canvas.drawLine(centerX, centerY + gap, centerX, centerY + gap + halfSize, paint)
            canvas.drawLine(centerX - gap, centerY, centerX - gap - halfSize, centerY, paint)
            canvas.drawLine(centerX + gap, centerY, centerX + gap + halfSize, centerY, paint)

            // Outer circle
            if (circleEnabled) {
                canvas.drawCircle(centerX, centerY, halfSize + gap, paint)
            }

            // Center dot
            if (dotEnabled) {
                paint.style = Paint.Style.FILL
                canvas.drawCircle(centerX, centerY, crosshairThickness * 1.5f, paint)
                paint.style = Paint.Style.STROKE
            }
        }
    }
}
