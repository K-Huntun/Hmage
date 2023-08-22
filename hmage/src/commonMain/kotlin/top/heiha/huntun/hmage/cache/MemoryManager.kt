package top.heiha.huntun.hmage.cache

internal expect class MemoryManager {
    fun getAvailableMemory(): Long
}

internal expect fun memoryManager(): MemoryManager