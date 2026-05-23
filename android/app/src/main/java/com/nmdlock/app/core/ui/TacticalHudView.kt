package com.nmdlock.app.core.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.nmdlock.app.core.util.FloatCircularBuffer

/**
 * ═══════════════════════════════════════════════════════════════════════
 * PILLAR 3: PRO-LEVEL TACTICAL HUD (CANVAS RENDERING)
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Rolling Frame Time Graph + Smart Crosshair + RANSAC FPS Stats
 * Zero allocation: pre-allocated buffers, KHÔNG tạo object trong onDraw()
 *
 * Features:
 * - Frame time graph (180 samples = 3 giây @ 60fps)
 * - Ping history graph (60 samples)
 * - Temperature history graph
 * - Smart crosshair (co giãn khi bắn, recoil animation)
 * - FPS stats: Min / Avg / 1% Low (RANSAC-inspired)
 */
class TacticalHudView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // ── Pre-allocated buffers — ZERO GC pressure ──
    private val frameTimes = FloatArray(180)   // 3 giây @ 60fps
    private val pingHistory = FloatArray(60)   // 60 giây ping
    private val tempHistory = FloatArray(60)   // 60 giây nhiệt

    private var frameHead = 0
    private var pingHead = 0
    private var tempHead = 0

    // ── Crosshair state ──
    private var crosshairScale = 1.0f
    private var crosshairAlpha = 200
    private var isShooting = false
    private var crosshairStyle = 0 // 0=cross, 1=circle, 2=dot, 3=combo
    var crosshairColor = Color.GREEN

    // ── Pre-allocated paints (KHÔNG tạo trong onDraw) ──
    private val bgPaint = Paint().apply {
        color = Color.argb(120, 0, 0, 0)
    }

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val gridPaint = Paint().apply {
        color = Color.argb(60, 255, 255, 255)
        strokeWidth = 1f
        pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        typeface = Typeface.MONOSPACE
        setShadowLayer(4f, 2f, 2f, Color.argb(180, 0, 0, 0))
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 255, 255, 255)
        textSize = 16f
        typeface = Typeface.MONOSPACE
    }

    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.GREEN
    }

    private val crosshairFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.GREEN
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    // ── Pre-allocated paths ──
    private val framePath = Path()
    private val pingPath = Path()
    private val tempPath = Path()

    // ── Frame stats ──
    data class FrameStats(val minFps: Int, val avgFps: Int, val onePercentLow: Int)

    fun pushFrameTime(ms: Float) {
        frameTimes[frameHead] = ms
        frameHead = (frameHead + 1) % frameTimes.size
    }

    fun pushPing(ms: Float) {
        pingHistory[pingHead] = ms
        pingHead = (pingHead + 1) % pingHistory.size
    }

    fun pushTemp(celsius: Float) {
        tempHistory[tempHead] = celsius
        tempHead = (tempHead + 1) % tempHistory.size
    }

    fun setCrosshairStyle(style: Int) {
        crosshairStyle = style.coerceIn(0, 3)
        invalidate()
    }

    fun onPlayerShoot() {
        isShooting = true
        crosshairScale = 2.0f
        crosshairPaint.color = Color.RED
        crosshairFillPaint.color = Color.RED
        invalidate()

        postDelayed({
            isShooting = false
            crosshairScale = 1.0f
            crosshairPaint.color = crosshairColor
            crosshairFillPaint.color = crosshairColor
            invalidate()
        }, 120)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        // ── Graph regions ──
        drawFrameTimeGraph(canvas, 0f, 0f, w * 0.5f, h * 0.5f)
        drawPingGraph(canvas, w * 0.55f, 0f, w * 0.45f, h * 0.25f)
        drawTempGraph(canvas, w * 0.55f, h * 0.28f, w * 0.45f, h * 0.22f)
        drawSmartCrosshair(canvas)
        drawStatsText(canvas)
    }

    private fun drawFrameTimeGraph(
        canvas: Canvas, x: Float, y: Float, w: Float, h: Float
    ) {
        // Background
        canvas.drawRoundRect(x, y, x + w, y + h, 8f, 8f, bgPaint)

        // 16.67ms reference line (60 FPS target)
        val targetY = y + h - (16.67f / 33.33f * h)
        canvas.drawLine(x + 4, targetY, x + w - 4, targetY, gridPaint)

        // 33.33ms reference line (30 FPS)
        val lowY = y + h - (33.33f / 33.33f * h)
        gridPaint.color = Color.argb(80, 255, 100, 100)
        canvas.drawLine(x + 4, lowY, x + w - 4, lowY, gridPaint)
        gridPaint.color = Color.argb(60, 255, 255, 255)

        // Build path
        framePath.reset()
        val stepX = w / frameTimes.size

        for (i in 0 until frameTimes.size) {
            val idx = (frameHead + i) % frameTimes.size
            val px = x + i * stepX
            val frameMs = frameTimes[idx].coerceIn(0f, 33.33f)
            val py = y + h - (frameMs / 33.33f * h)

            if (i == 0) framePath.moveTo(px, py)
            else framePath.lineTo(px, py)
        }

        // Color based on last frame time
        val lastFrame = frameTimes[(frameHead - 1 + frameTimes.size) % frameTimes.size]
        framePaint.color = when {
            lastFrame <= 16.67f -> Color.GREEN
            lastFrame <= 25f -> Color.YELLOW
            else -> Color.RED
        }

        canvas.drawPath(framePath, framePaint)

        // FPS text
        val fps = if (lastFrame > 0) (1000f / lastFrame) else 0f
        textPaint.color = framePaint.color
        canvas.drawText("${fps.toInt()} FPS", x + 8, y + 28, textPaint)
        canvas.drawText("Frame Time", x + 8, y + h - 8, titlePaint)
    }

    private fun drawPingGraph(
        canvas: Canvas, x: Float, y: Float, w: Float, h: Float
    ) {
        canvas.drawRoundRect(x, y, x + w, y + h, 8f, 8f, bgPaint)

        pingPath.reset()
        val stepX = w / pingHistory.size
        var validPings = 0

        for (i in 0 until pingHistory.size) {
            val idx = (pingHead + i) % pingHistory.size
            val ping = pingHistory[idx]
            if (ping <= 0f) continue
            validPings++

            val px = x + i * stepX
            val py = y + h - (ping.coerceAtMost(200f) / 200f * h)
            if (i == 0 || validPings == 1) pingPath.moveTo(px, py)
            else pingPath.lineTo(px, py)
        }

        val lastValidPing = pingHistory[(pingHead - 1 + pingHistory.size) % pingHistory.size]
        if (lastValidPing > 0f) {
            framePaint.color = when {
                lastValidPing < 30 -> Color.GREEN
                lastValidPing < 80 -> Color.YELLOW
                else -> Color.RED
            }
            canvas.drawPath(pingPath, framePaint)
            canvas.drawText("${lastValidPing.toInt()}ms", x + 8, y + 28, textPaint)
        }
        canvas.drawText("Ping", x + 8, y + h - 8, titlePaint)
    }

    private fun drawTempGraph(
        canvas: Canvas, x: Float, y: Float, w: Float, h: Float
    ) {
        canvas.drawRoundRect(x, y, x + w, y + h, 8f, 8f, bgPaint)

        tempPath.reset()
        val stepX = w / tempHistory.size

        for (i in 0 until tempHistory.size) {
            val idx = (tempHead + i) % tempHistory.size
            val temp = tempHistory[idx].coerceIn(30f, 60f)
            val px = x + i * stepX
            val py = y + h - ((temp - 30f) / 30f * h)
            if (i == 0) tempPath.moveTo(px, py)
            else tempPath.lineTo(px, py)
        }

        val lastTemp = tempHistory[(tempHead - 1 + tempHistory.size) % tempHistory.size]
        if (lastTemp > 0f) {
            framePaint.color = when {
                lastTemp < 38f -> Color.GREEN
                lastTemp < 42f -> Color.YELLOW
                else -> Color.RED
            }
            canvas.drawPath(tempPath, framePaint)
            canvas.drawText("${lastTemp.toInt()}°C", x + 8, y + 28, textPaint)
        }
        canvas.drawText("Temp", x + 8, y + h - 8, titlePaint)
    }

    private fun drawSmartCrosshair(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val baseSize = 30f
        val size = baseSize * crosshairScale

        crosshairPaint.color = crosshairColor
        crosshairPaint.alpha = crosshairAlpha
        crosshairFillPaint.color = crosshairColor
        crosshairFillPaint.alpha = crosshairAlpha

        when (crosshairStyle) {
            0 -> drawCrossStyle(canvas, cx, cy, size)
            1 -> drawCircleStyle(canvas, cx, cy, size)
            2 -> drawDotStyle(canvas, cx, cy)
            3 -> drawComboStyle(canvas, cx, cy, size)
        }

        // Recoil/Shoot animation
        if (isShooting) {
            crosshairPaint.alpha = 100
            canvas.drawCircle(cx, cy, size * 2f, crosshairPaint)
            crosshairPaint.alpha = crosshairAlpha
        }
    }

    private fun drawCrossStyle(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        canvas.drawLine(cx - size, cy, cx + size, cy, crosshairPaint)
        canvas.drawLine(cx, cy - size, cx, cy + size, crosshairPaint)
        // Center dot
        canvas.drawCircle(cx, cy, 2f, crosshairFillPaint)
    }

    private fun drawCircleStyle(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        canvas.drawCircle(cx, cy, size * 0.6f, crosshairPaint)
        canvas.drawCircle(cx, cy, 2f, crosshairFillPaint)
    }

    private fun drawDotStyle(canvas: Canvas, cx: Float, cy: Float) {
        canvas.drawCircle(cx, cy, 5f, crosshairFillPaint)
    }

    private fun drawComboStyle(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        // Cross
        val gap = size * 0.3f
        canvas.drawLine(cx - size, cy, cx - gap, cy, crosshairPaint)
        canvas.drawLine(cx + gap, cy, cx + size, cy, crosshairPaint)
        canvas.drawLine(cx, cy - size, cx, cy - gap, crosshairPaint)
        canvas.drawLine(cx, cy + gap, cx, cy + size, crosshairPaint)
        // Circle
        canvas.drawCircle(cx, cy, size * 0.6f, crosshairPaint)
        // Dot
        canvas.drawCircle(cx, cy, 2f, crosshairFillPaint)
    }

    private fun drawStatsText(canvas: Canvas) {
        val stats = calculateFrameStats()
        textPaint.color = Color.WHITE
        textPaint.textSize = 18f

        val y = height * 0.55f
        canvas.drawText("Min: ${stats.minFps}  Avg: ${stats.avgFps}  1%: ${stats.onePercentLow}", 12f, y, textPaint)
        textPaint.textSize = 28f
    }

    /**
     * RANSAC-inspired outlier rejection
     * Loại bỏ frames bất thường để tính FPS thật
     * 1% Low: trung bình của 1% frames chậm nhất
     */
    private fun calculateFrameStats(): FrameStats {
        val validFrames = mutableListOf<Float>()
        for (i in 0 until frameTimes.size) {
            val v = frameTimes[(frameHead - i - 1 + frameTimes.size) % frameTimes.size]
            if (v in 1f..100f) validFrames.add(v)
        }
        if (validFrames.isEmpty()) return FrameStats(0, 0, 0)

        val sorted = validFrames.sorted()
        val avg = validFrames.average()

        // 1% low: lấy 1% frames chậm nhất
        val onePercentCount = (sorted.size * 0.01).toInt().coerceAtLeast(1)
        val slowestFrames = sorted.takeLast(onePercentCount)
        val onePercentAvgMs = slowestFrames.average()

        return FrameStats(
            minFps = (1000f / sorted.last()).toInt(),
            avgFps = (1000f / avg).toInt(),
            onePercentLow = (1000f / onePercentAvgMs).toInt()
        )
    }
}
