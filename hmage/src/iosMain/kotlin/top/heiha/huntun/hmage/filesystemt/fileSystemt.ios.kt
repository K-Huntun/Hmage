package top.heiha.huntun.hmage.filesystemt

import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

internal actual val fileSystem = FileSystem.SYSTEM

internal actual val cachePath = (NSSearchPathForDirectoriesInDomains(
    NSCachesDirectory,
    NSUserDomainMask,
    true
).first() as String).toPath()