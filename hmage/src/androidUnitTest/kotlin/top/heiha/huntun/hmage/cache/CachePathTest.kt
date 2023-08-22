package top.heiha.huntun.hmage.cache

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import top.heiha.huntun.hmage.HmageContenxt
import top.heiha.huntun.hmage.filesystemt.cachePath
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CachePathTest {
    @BeforeTest
    fun setUp() {
        HmageContenxt.application = RuntimeEnvironment.getApplication()
    }

    @Test
    fun getCachePath() {
        assertTrue(cachePath.toString().isNotBlank())
    }
}