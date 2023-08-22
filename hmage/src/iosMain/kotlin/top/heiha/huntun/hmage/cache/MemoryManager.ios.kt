package top.heiha.huntun.hmage.cache

import platform.Foundation.NSProcessInfo

internal actual class MemoryManager {
    actual fun getAvailableMemory(): Long {
        val totalMemory = NSProcessInfo.processInfo.physicalMemory
        return (totalMemory.toLong() * 0.5 * 0.25).toLong()
    }

}

internal actual fun memoryManager(): MemoryManager {
    return MemoryManager()
}