package qiaqia

import top.heiha.huntun.hmage.cache.Lru
import kotlin.test.Test
import kotlin.test.assertEquals

class LruTest {

    @Test
    fun testSet() {
        val key = 0
        val value = 0
        val lru = Lru<Int, Int>(1)
        lru[key] = value
        assertEquals(value, lru[key])
    }

    @Test
    fun testOverMaxSize() {
        val data = intArrayOf(0, 1)
        val lru = Lru<Int, Int>(1)
        data.forEachIndexed { k, v ->
            lru[k] = v
        }
        assertEquals(null, lru[0])
        assertEquals(1, lru[1])
    }

    @Test
    fun testPriority() {
        val lru = Lru<Int, Int>(2)
        sequence {
            yield(0 to 0)
            yield(1 to 1)
            yield(0 to 0)
            yield(2 to 2)
        }.forEach { (k, v) ->
            lru[k] = v
        }
        assertEquals(0, lru[0])
        assertEquals(null, lru[1])
        assertEquals(2, lru[2])
    }

    @Test
    fun testTrim() {
        val data = intArrayOf(0, 1, 2)
        val lru = Lru<Int, Int>(3)
        data.forEachIndexed { k, v ->
            lru[k] = v
        }
        lru.maxSize(2)
        assertEquals(null, lru[0])
        assertEquals(1, lru[1])
        assertEquals(2, lru[2])
    }

    @Test
    fun testPriorityAfterAccess() {
        val data = intArrayOf(0, 1, 2)
        val lru = Lru<Int, Int>(3)
        data.forEachIndexed { k, v ->
            lru[k] = v
        }

        val valueOfIndex0 = lru[1]
        lru[3] = 3
        lru[4] = 4
        assertEquals(null, lru[0])
        assertEquals(1, lru[1])
        assertEquals(null, lru[2])
        assertEquals(3, lru[3])
        assertEquals(4, lru[4])
    }
}