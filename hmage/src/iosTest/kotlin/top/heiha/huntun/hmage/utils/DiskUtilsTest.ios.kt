package top.heiha.huntun.hmage.utils

import kotlin.test.Test
import kotlin.test.assertTrue


class DiskUtilsTest {

    @Test
    fun getAvailableCacheSpaceTest() {
        val availableCacheSpace = getAvailableCacheSpace()
        println("availableCacheSpace: $availableCacheSpace")
        assertTrue(availableCacheSpace > 0)
    }
}


