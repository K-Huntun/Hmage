package qiaqia

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertTrue

class DirectoryExistTest {
    @Test
    fun checkDirectoryExist() {
        val fileSystem = FakeFileSystem()
        val path = "/Users/qiaqia/-12345535".toPath()
        fileSystem.createDirectories(path)
        assertTrue(fileSystem.exists(path))
    }
}