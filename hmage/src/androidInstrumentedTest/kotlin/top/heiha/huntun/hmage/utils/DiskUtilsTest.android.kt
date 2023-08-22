package top.heiha.huntun.hmage.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class DiskUtilsTest {

    @Test
    fun getAvailableCacheSpaceTest() {
        val availableCacheSpace = getAvailableCacheSpace()
        assertTrue(availableCacheSpace > 0)
    }
}


