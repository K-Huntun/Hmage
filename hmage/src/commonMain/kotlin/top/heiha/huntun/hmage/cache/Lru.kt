package top.heiha.huntun.hmage.cache

internal class Lru<K, V>(maxSize: Long) {
    private var maxSize: Long = maxSize
    private var size: Long = 0L

    /**
     * +-----+-----+
     * |  a  |  b  |
     * +-----+-----+
     * a will be removed if a new value is added
     * b is recently accessed
     *
     */
    private val bulk = LinkedHashMap<K, V>()
    var sizeOf: (K, V) -> Long = { _, _ -> 1L }

    operator fun get(key: K): V? {
        val value = bulk.remove(key)
        if (value != null) {
            bulk[key] = value
        }
        return value
    }

    operator fun set(key: K, value: V) {
        val oldValue = bulk.remove(key)
        bulk[key] = value
        val updatedSize = if (oldValue != null) {
            size - sizeOf(key, oldValue) + sizeOf(key, value)
        } else {
            size + sizeOf(key, value)
        }
        size = update(updatedSize, maxSize)
    }

    fun maxSize(maxSize: Long) {
        if (maxSize < this.maxSize) {
            size = update(size, maxSize)
        }
        this.maxSize = maxSize
    }

    private fun update(totalSize: Long, maxSize: Long): Long {
        if (totalSize <= maxSize) return totalSize

        var totalSize = totalSize
        val iterator = bulk.iterator()
        while (totalSize > maxSize) {
            if (iterator.hasNext()) {
                val (key, value) = iterator.next()
                iterator.remove()
                val itemSize = sizeOf(key, value)
                totalSize -= itemSize
            }
        }

        return totalSize
    }
}