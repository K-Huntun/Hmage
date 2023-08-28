package top.heiha.huntun.hmage.cache

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class Lru<K, V>(maxSize: Long) {
    private var maxSize: Long = maxSize
    private var size: Long = 0L
    private val lock = Mutex()

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
        return runBlocking {
            lock.withLock {
                val value = bulk.remove(key)
                if (value != null) {
                    bulk[key] = value
                }
                value
            }
        }
    }

    operator fun set(key: K, value: V) {
        runBlocking {
            val oldValue = lock.withLock {
                val oldValue = bulk.remove(key)
                bulk[key] = value
                oldValue
            }
            if (oldValue != value) {
                val updatedSize = if (oldValue != null) {
                    size - sizeOf(key, oldValue) + sizeOf(key, value)
                } else {
                    size + sizeOf(key, value)
                }
                size = update(updatedSize, maxSize)
            }
        }
    }

    internal operator fun minusAssign(key: K) {
        runBlocking {
            val value = bulk.remove(key)
            if (value != null) {
                size -= sizeOf(key, value)
            }
        }
    }

    fun maxSize(maxSize: Long) {
        runBlocking {
            lock.withLock {
                if (maxSize < this@Lru.maxSize) {
                    size = update(size, maxSize)
                }
                this@Lru.maxSize = maxSize
            }
        }
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

    suspend fun asIterable(): Iterable<Pair<K, V>>  {
        return lock.withLock {
            bulk.entries.map { Pair(it.key, it.value) }
        }
    }

}