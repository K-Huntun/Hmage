package top.heiha.huntun.hmage.filesystemt

import okio.FileSystem
import okio.Path

internal expect val fileSystem: FileSystem

internal expect val cachePath: Path