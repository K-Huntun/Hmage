package top.heiha.huntun.hmage.utils

import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask


internal actual fun getAvailableCacheSpace(): Long {
    val fileManager = NSFileManager.defaultManager
    val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    val docDirectory = paths.first().toString()
    val fsAttr = fileManager.attributesOfFileSystemForPath(docDirectory, null)!!
    val freeSize = fsAttr[NSFileSystemFreeSize] as Long
    return freeSize
}
