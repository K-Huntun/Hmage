package top.heiha.huntun.hmage.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import okio.Path.Companion.toPath
import org.jetbrains.skia.Image
import platform.Foundation.NSBundle
import top.heiha.huntun.hmage.filesystemt.fileSystem

internal actual fun ByteArray.toImageBitmap(): ImageBitmap {
    return Image.makeFromEncoded(this).toComposeImageBitmap()
}

internal actual fun readResourceImage(image: String): ByteArray {
    val separatorIndex = image.indexOfLast { it == '.' }
    if (separatorIndex < 0 || separatorIndex == image.length - 1) {
        throw IllegalArgumentException("Name of image must contain type suffix")
    } else if (separatorIndex == 0) {
        throw IllegalArgumentException("Name of image must contain file name without type suffix")
    }
    return readBytes(image.substring(0, separatorIndex), image.substring(separatorIndex + 1, image.length))
}

private fun readBytes(name: String, type: String): ByteArray {
    val path = NSBundle.mainBundle.pathForResource(name, type, "compose-resources")!!.toPath()
    return fileSystem.read(path) {
        readByteArray()
    }

}