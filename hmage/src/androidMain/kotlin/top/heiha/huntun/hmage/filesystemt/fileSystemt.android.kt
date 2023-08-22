package top.heiha.huntun.hmage.filesystemt

import okio.FileSystem
import okio.Path.Companion.toOkioPath
import top.heiha.huntun.hmage.HmageContenxt


internal actual val fileSystem = FileSystem.SYSTEM

internal actual val cachePath = HmageContenxt.application.cacheDir.toOkioPath()