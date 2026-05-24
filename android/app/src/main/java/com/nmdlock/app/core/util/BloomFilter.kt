package com.nmdlock.app.core.util

/**
 * Bloom Filter — Dedup package names khi kill apps
 * Dùng bitmap + multiple hash functions để kiểm tra "có thể đã thấy" phần tử
 * False positive có thể xảy ra, false negative = không
 *
 * Tối ưu: 1024 bit, 3 hash functions
 */
class BloomFilter(
    private val bitSetSize: Int = 1024,
    private val numHashFunctions: Int = 3
) {
    private val bitSet = BitSet(bitSetSize)

    fun put(element: String) {
        val hashes = hash(element)
        for (h in hashes) {
            bitSet.set(h)
        }
    }

    fun mightContain(element: String): Boolean {
        val hashes = hash(element)
        return hashes.all { bitSet.get(it) }
    }

    fun clear() {
        bitSet.clear()
    }

    /**
     * Tạo multiple hash values từ element
     * Dùng Double Hashing: h_i(x) = (h1(x) + i * h2(x)) mod m
     */
    private fun hash(element: String): IntArray {
        val hash1 = simpleHash(element, 0x45D9F3B)
        val hash2 = simpleHash(element, -0x61C88647)

        return IntArray(numHashFunctions) { i ->
            ((hash1 + i.toLong() * hash2) % bitSetSize).toInt().let {
                if (it < 0) -it else it
            }
        }
    }

    private fun simpleHash(element: String, seed: Int): Int {
        var hash = seed
        for (char in element) {
            hash = hash * 31 + char.code
        }
        return hash
    }
}

/**
 * Đơn giản hóa: dùng java.util.BitSet
 */
private class BitSet(private val size: Int) {
    private val bits = BooleanArray(size)

    fun set(index: Int) {
        bits[index % size] = true
    }

    fun get(index: Int): Boolean {
        return bits[index % size]
    }

    fun clear() {
        bits.fill(false)
    }
}
