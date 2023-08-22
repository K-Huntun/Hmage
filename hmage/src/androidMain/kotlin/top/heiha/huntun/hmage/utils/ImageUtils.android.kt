package top.heiha.huntun.hmage.utils

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import okio.Path
import okio.buffer
import okio.source
import top.heiha.huntun.hmage.HmageContenxt

internal actual fun ByteArray.toImageBitmap(): ImageBitmap {
    return BitmapFactory.decodeByteArray(this, 0, size).asImageBitmap()
}

internal actual fun readResourceImage(image: String): ByteArray {
    return HmageContenxt::class.java.getResource("${Path.DIRECTORY_SEPARATOR}$image").openStream().source().buffer()
        .readByteArray()
}