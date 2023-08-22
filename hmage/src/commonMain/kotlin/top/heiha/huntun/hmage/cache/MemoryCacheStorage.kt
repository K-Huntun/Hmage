package top.heiha.huntun.hmage.cache

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import com.heiha.huntun.hog.logd
import top.heiha.huntun.hmage.TAG

internal class MemoryCacheStorage {
    private val cache =
        Lru<String, ImageBitmap>((memoryManager().getAvailableMemory() * 0.25).toLong()).apply {
            sizeOf = { k, v ->
                val bytesPerPixel = when (v.config) {
                    ImageBitmapConfig.Argb8888 -> 4
                    ImageBitmapConfig.Alpha8 -> 1
                    ImageBitmapConfig.Rgb565 -> 2
                    ImageBitmapConfig.F16 -> 8
                    ImageBitmapConfig.Gpu -> 1
                    else -> throw IllegalStateException()
                }
                // 向上按4取整
                ((v.width * bytesPerPixel + 4 - 1) / 4).toLong() * 4
            }
        }

    fun put(key: String, value: ImageBitmap): ImageBitmap {
        logd(TAG, "store memory cache, key: $key")
        cache[key] = value
        return value
    }

    fun get(key: String): ImageBitmap? {
        logd(TAG, "get memory cache, key: $key")
        return cache[key]?.apply {
            logd(TAG, "hit memory cache, key: $key")
        }
    }
}

internal val memoryCache = MemoryCacheStorage()

