package top.heiha.huntun.hmage.cache

import top.heiha.huntun.hmage.filesystemt.cachePath
import kotlin.test.Test
import kotlin.test.assertTrue

class CachePathTest {

    @Test
    fun getCachePath() {
        assertTrue(cachePath.toString().isNotBlank())
    }
}