package top.heiha.huntun.hmage.cache

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class MemoryManagerTest {

    @Test
    fun getAvailableMemory() {
        var availableMemory: Long
        try {
            availableMemory = memoryManager().getAvailableMemory()
        } catch (t: Throwable) {
            fail("failed to get available memory", t)
        }

        assertTrue(availableMemory > 0)
    }
}