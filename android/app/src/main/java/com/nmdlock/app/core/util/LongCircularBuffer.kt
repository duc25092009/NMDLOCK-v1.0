package com.nmdlock.app.core.util

/**
 * Circular buffer cho Long values (timestamps)
 */
class LongCircularBuffer(private val capacity: Int) {
    private val buffer = LongArray(capacity)
    private var head = 0
    private var _size = 0

    val size: Int get() = _size

    fun add(value: Long) {
        buffer[head] = value
        head = (head + 1) % capacity
        if (_size < capacity) _size++
    }

    operator fun get(index: Int): Long {
        if (index < 0 || index >= _size) return 0L
        val idx = (head - _size + index + capacity) % capacity
        return buffer[idx]
    }

    fun latest(): Long {
        if (_size == 0) return 0L
        return buffer[(head - 1 + capacity) % capacity]
    }

    fun clear() {
        head = 0
        _size = 0
    }
}
