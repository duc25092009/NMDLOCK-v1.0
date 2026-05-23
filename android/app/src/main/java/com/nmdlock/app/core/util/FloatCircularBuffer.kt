package com.nmdlock.app.core.util

/**
 * Circular buffer / Ring buffer — Zero allocation rolling data structure
 * Dùng cho thermal history, FPS history, ping history
 */
class FloatCircularBuffer(private val capacity: Int) {
    private val buffer = FloatArray(capacity)
    private var head = 0
    private var _size = 0

    val size: Int get() = _size

    fun add(value: Float) {
        buffer[head] = value
        head = (head + 1) % capacity
        if (_size < capacity) _size++
    }

    operator fun get(index: Int): Float {
        if (index < 0 || index >= _size) return 0f
        val idx = (head - _size + index + capacity) % capacity
        return buffer[idx]
    }

    fun latest(): Float {
        if (_size == 0) return 0f
        val idx = (head - 1 + capacity) % capacity
        return buffer[idx]
    }

    fun toList(): List<Float> {
        val result = mutableListOf<Float>()
        for (i in 0 until _size) {
            result.add(get(i))
        }
        return result
    }

    fun average(): Float {
        if (_size == 0) return 0f
        var sum = 0.0
        for (i in 0 until _size) {
            sum += get(i).toDouble()
        }
        return (sum / _size).toFloat()
    }

    fun clear() {
        head = 0
        _size = 0
    }
}
