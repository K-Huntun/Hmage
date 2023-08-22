package top.heiha.huntun.hmage.utils

import android.os.Environment
import android.os.StatFs


internal actual fun getAvailableCacheSpace(): Long {
    return getAvailableInternalDiskSpace()
}

internal fun getAvailableInternalDiskSpace(): Long {
    val path = Environment.getDataDirectory().path
    val stat = StatFs(path)
    return stat.availableBytes
}

internal fun getAvailableExternalDiskSpace(): Long {
    val path = Environment.getExternalStorageDirectory().path
    val stat = StatFs(path)
    val blockSize = stat.blockSizeLong
    val availableBlocks = stat.availableBlocksLong
    return blockSize * availableBlocks
}