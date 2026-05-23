package com.nmdlock.app.core.util

/**
 * Kalman Filter — Smooth sensor noise cho nhiệt độ/ping
 * Dùng để lọc nhiễu đo lường, ước lượng giá trị thật từ measurements nhiễu
 *
 * Công thức:
 * - Predict:  x̂ₖ = x̂ₖ₋₁,     Pₖ = Pₖ₋₁ + Q
 * - Update:   Kₖ = Pₖ / (Pₖ + R)
 *             x̂ₖ = x̂ₖ + Kₖ(zₖ - x̂ₖ)
 *             Pₖ = (1 - Kₖ)Pₖ
 */
class KalmanFilter(
    private val processNoise: Double = 0.1,   // Q: Độ tin cậy của model
    private val measurementNoise: Double = 5.0 // R: Độ tin cậy của sensor
) {
    private var x = 0.0  // Estimated state (giá trị ước lượng)
    private var p = 1.0  // Estimation error covariance (sai số ước lượng)

    fun filter(measurement: Double): Double {
        // Predict step: dự đoán trạng thái tiếp theo
        val pPredict = p + processNoise

        // Update step: hiệu chỉnh dựa trên measurement
        val kalmanGain = pPredict / (pPredict + measurementNoise)
        x = x + kalmanGain * (measurement - x)
        p = (1.0 - kalmanGain) * pPredict

        return x
    }

    fun filterFloat(measurement: Float): Float {
        return filter(measurement.toDouble()).toFloat()
    }

    fun reset(initialValue: Double = 0.0) {
        x = initialValue
        p = 1.0
    }
}
