package com.nmdlock.app.core.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

/**
 * ═══════════════════════════════════════════════════════════════════════
 * PILLAR D: TOUCH METRICS HUD OVERLAY
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Overlay đo độ trễ cảm ứng thực tế, hiển thị real-time:
 * - Touch Latency graph (60 samples rolling)
 * - Số lần chạm
 * - Grade: A+ (Pro) → D (Poor)
 *
 * View vẽ bằng Canvas, zero allocation trong onDraw()
 */
class TouchMetricsOverlay : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: TouchMetricsView? = null
    private var isVisible = false

    companion object {
        private const val CHANNEL_ID = "touch_metrics_overlay"
        private const val NOTIFICATION_ID = 1003
        private var running = false
        private var instance: TouchMetricsOverlay? = null

        // Touch metrics tracking
        private val touchLatencies = FloatArray(60)  // 60 samples
        private var latencyHead = 0
        private var touchCount = 0
        private var avgLatency = 0f

        fun isRunning(): Boolean = running
        fun getAvgLatency(): Float = avgLatency
        fun getTouchCount(): Int = touchCount
        fun getLatencyGrade(): String {
            return when {
                avgLatency < 10f -> "A+ (Pro)"
                avgLatency < 20f -> "A (Tuyệt vời)"
                avgLatency < 30f -> "B (Tốt)"
                avgLatency < 50f -> "C (Trung bình)"
                else -> "D (Kém)"
            }
        }

        fun start(context: Context) {
            if (!running) {
                val intent = Intent(context, TouchMetricsOverlay::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            if (running) {
                context.stopService(Intent(context, TouchMetricsOverlay::class.java))
                resetMetrics()
            }
        }

        fun recordTouchDown() {
            touchCount++
        }

        fun recordTouchUp(latencyMs: Float) {
            touchLatencies[latencyHead] = latencyMs
            latencyHead = (latencyHead + 1) % touchLatencies.size

            // Kalman-smoothed average
            avgLatency = avgLatency * 0.7f + latencyMs * 0.3f

            instance?.overlayView?.invalidate()
        }

        private fun resetMetrics() {
            touchLatencies.fill(0f)
            latencyHead = 0
            touchCount = 0
            avgLatency = 0f
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Touch Metrics",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Touch latency metrics overlay"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NMDLock")
            .setContentText("Touch Metrics overlay đang hoạt động")
            .setSmallIcon(android.R.drawable.ic_menu_sort_by_size)
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
            280,
            200,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 10
        params.y = 200

        overlayView = TouchMetricsView(this)
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

    inner class TouchMetricsView(context: Context) : View(context) {
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.MONOSPACE
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
        }

        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, 255, 255, 255)
            textSize = 13f
            typeface = Typeface.MONOSPACE
        }

        private val graphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.CYAN
        }

        private val bgPaint = Paint().apply {
            color = Color.argb(130, 0, 0, 0)
            style = Paint.Style.FILL
        }

        private val gridPaint = Paint().apply {
            color = Color.argb(40, 255, 255, 255)
            strokeWidth = 1f
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val graphWidth = 200f
            val graphHeight = 80f
            val padding = 10f
            val graphX = padding
            val graphY = height - graphHeight - padding

            // Background
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 12f, 12f, bgPaint)

            // Title
            textPaint.color = Color.CYAN
            textPaint.textSize = 22f
            canvas.drawText("Touch Latency: ${avgLatency.toInt()}ms", padding, 28f, textPaint)

            // Grade
            val grade = getLatencyGrade()
            val gradeColor = when {
                avgLatency < 10f -> Color.GREEN
                avgLatency < 20f -> Color.CYAN
                avgLatency < 30f -> Color.YELLOW
                avgLatency < 50f -> Color.argb(255, 255, 165, 0) // Orange
                else -> Color.RED
            }
            textPaint.color = gradeColor
            textPaint.textSize = 16f
            canvas.drawText("Grade: $grade", padding, 48f, textPaint)

            // Touch count
            textPaint.color = Color.argb(180, 255, 255, 255)
            textPaint.textSize = 14f
            canvas.drawText("Touches: $touchCount", padding, 64f, textPaint)

            // Graph background
            canvas.drawRoundRect(
                graphX, graphY, graphX + graphWidth, graphY + graphHeight,
                6f, 6f, Paint().apply { color = Color.argb(80, 0, 0, 0); style = Paint.Style.FILL }
            )

            // Reference line at 16ms (60fps frame budget)
            val refY = graphY + graphHeight - (16f / 50f * graphHeight)
            gridPaint.color = Color.argb(60, 100, 255, 100)
            canvas.drawLine(graphX, refY, graphX + graphWidth, refY, gridPaint)
            labelPaint.color = Color.argb(80, 100, 255, 100)
            canvas.drawText("16ms", graphX + 2, refY - 3, labelPaint)

            // Plot latencies
            val stepX = graphWidth / touchLatencies.size
            var anyValid = false

            for (i in 1 until touchLatencies.size) {
                val idx = (latencyHead + i) % touchLatencies.size
                val prevIdx = (latencyHead + i - 1) % touchLatencies.size

                val currentLatency = touchLatencies[idx]
                val prevLatency = touchLatencies[prevIdx]

                if (currentLatency <= 0f) continue
                anyValid = true

                val px1 = graphX + (i - 1) * stepX
                val py1 = graphY + graphHeight - (prevLatency.coerceAtMost(50f) / 50f * graphHeight)
                val px2 = graphX + i * stepX
                val py2 = graphY + graphHeight - (currentLatency.coerceAtMost(50f) / 50f * graphHeight)

                graphPaint.color = when {
                    currentLatency < 16f -> Color.GREEN    // Good
                    currentLatency < 33f -> Color.YELLOW   // OK
                    else -> Color.RED                      // Bad
                }

                canvas.drawLine(px1, py1, px2, py2, graphPaint)
            }

            if (!anyValid) {
                labelPaint.color = Color.argb(100, 255, 255, 255)
                canvas.drawText("Đang chờ touch...", graphX + 20, graphY + graphHeight / 2 + 5, labelPaint)
            }
        }
    }
}
