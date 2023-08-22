package top.heiha.huntun.hmage.cache

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import com.heiha.huntun.hog.logd
import top.heiha.huntun.hmage.HmageContenxt
import top.heiha.huntun.hmage.TAG

internal actual class MemoryManager(private val application: Application) {
    /**
     * The available memory on the system.
     * This number should not be considered absolute: due to the nature of the kernel,
     * a significant portion of this memory is actually in use and needed for the overall system to run well.
     * @return unit byte
     */
    actual fun getAvailableMemory(): Long {
        val activityManager = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return ActivityManager.MemoryInfo().also {
            activityManager.getMemoryInfo(it)
        }.let {
            it.availMem.apply {
                logd(TAG, "availMem: $this")
            }
        }
    }
}

internal actual fun memoryManager() = MemoryManager(HmageContenxt.application)