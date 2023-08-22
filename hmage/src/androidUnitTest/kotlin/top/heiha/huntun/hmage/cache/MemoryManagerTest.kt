package top.heiha.huntun.hmage.cache

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
//import org.robolectric.RobolectricTestRunner
//import org.robolectric.RuntimeEnvironment
import top.heiha.huntun.hmage.HmageContenxt
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.fail

@RunWith(value = RobolectricTestRunner::class)
class MemoryManagerTest {

    @BeforeTest
    fun setUp() {
        HmageContenxt.init(RuntimeEnvironment.getApplication())
    }

    @Test
    fun getAvailableMemory() {
        try {
            memoryManager().getAvailableMemory()
        } catch (t: Throwable) {
            fail("failed to get available memory", t)
        }
    }
}