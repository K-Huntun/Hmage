package top.heiha.huntun.hmage.utils

import androidx.compose.ui.graphics.ImageBitmap

internal expect fun ByteArray.toImageBitmap(): ImageBitmap

internal expect fun readResourceImage(image: String): ByteArray