package top.heiha.huntun.hmage.utils

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import okio.buffer
import okio.source
import top.heiha.huntun.hmage.HmageContenxt

internal actual fun ByteArray.toImageBitmap(): ImageBitmap {
    return BitmapFactory.decodeByteArray(this, 0, size).asImageBitmap()
}

internal actual fun readResourceImage(image: String): ByteArray {
    val classLoader = Thread.currentThread().contextClassLoader ?: HmageContenxt::class.java.classLoader
    return classLoader.getResourceAsStream("$image").source().buffer()
        .readByteArray()
}