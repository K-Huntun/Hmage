package top.heiha.huntun.hmage.cache

import com.heiha.huntun.hog.LogLevel
import com.heiha.huntun.hog.logDelegate
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import top.heiha.huntun.hmage.utils.readResourceImage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class PersistentCacheStorageTest {
    private lateinit var storage: PersistentCacheStorage
    private lateinit var fakeFileSystem: FakeFileSystem
    private val cachePath = "/data/hmage/cache/network_cache".toPath()

    @BeforeTest
    fun setup() {
        logDelegate = { level: LogLevel, tag: String, msg: String, tr: Throwable? ->
            println("[${level.simpleName}] $tag $msg\n${tr?.stackTraceToString()}")
        }
        fakeFileSystem = FakeFileSystem()
        storage = PersistentCacheStorage(
            10 * 1024 * 1024,
            fakeFileSystem,
            cachePath
        )

    }

    @AfterTest
    fun tearDown() {
    }

    @Test
    fun testPut() {
        val url = Url("https://www.test.com/test.jpg")
        val data = CachedResponseData(
            url,
            HttpStatusCode.OK,
            GMTDate(Clock.System.now().toEpochMilliseconds() - 4000L),
            GMTDate(Clock.System.now().toEpochMilliseconds() - 3000L),
            version = HttpProtocolVersion.HTTP_2_0,
            expires = GMTDate(Clock.System.now().toEpochMilliseconds() + 24 * 3600 * 1000L),
            headers = HeadersBuilder().build(),
            varyKeys = emptyMap(),
            body = readResourceImage("placeholder.png")
        )
        runBlocking {
            cacheTestImage(url, data)
            assertTrue(
                fakeFileSystem.list(cachePath)
                    .find { it.name.startsWith(url.hashCode().toString()) } != null,
                "Cache file should exist.")
        }
    }


    @Test
    fun testGet() {
        val url = Url("https://www.test.com/test.jpg")
        val data = CachedResponseData(
            url,
            HttpStatusCode.OK,
            GMTDate(Clock.System.now().toEpochMilliseconds() - 4000L),
            GMTDate(Clock.System.now().toEpochMilliseconds() - 3000L),
            version = HttpProtocolVersion.HTTP_2_0,
            expires = GMTDate(Clock.System.now().toEpochMilliseconds() + 24 * 3600 * 1000L),
            headers = HeadersBuilder().build(),
            varyKeys = emptyMap(),
            body = readResourceImage("placeholder.png")
        )
        runBlocking {
            cacheTestImage(url, data)
            assertNotNull(storage.find(url, emptyMap()), "Should hit the cache!")
        }
    }

    @Test
    fun concurrencyReadTest() {
        val url = Url("https://www.test.com/test.jpg")
        val data = CachedResponseData(
            url,
            HttpStatusCode.OK,
            GMTDate(Clock.System.now().toEpochMilliseconds() - 4000L),
            GMTDate(Clock.System.now().toEpochMilliseconds() - 3000L),
            version = HttpProtocolVersion.HTTP_2_0,
            expires = GMTDate(Clock.System.now().toEpochMilliseconds() + 24 * 3600 * 1000L),
            headers = HeadersBuilder().build(),
            varyKeys = emptyMap(),
            body = readResourceImage("placeholder.png")
        )
        try {
            runBlocking {
                cacheTestImage(url, data)
                (0 until 100).forEach { _ ->
                    withContext(Dispatchers.IO) {
                        val byteArray = storage.find(url, emptyMap())!!.body
                        assertTrue(
                            byteArray.isNotEmpty(),
                            "The size of byte array should not empty"
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            fail("Get and decode failed", t)
        }

    }

    @Test
    fun concurrencyWriteTest() {
        val url = Url("https://www.test.com/test.jpg")
        val data = CachedResponseData(
            url,
            HttpStatusCode.OK,
            GMTDate(Clock.System.now().toEpochMilliseconds() - 4000L),
            GMTDate(Clock.System.now().toEpochMilliseconds() - 3000L),
            version = HttpProtocolVersion.HTTP_2_0,
            expires = GMTDate(Clock.System.now().toEpochMilliseconds() + 24 * 3600 * 1000L),
            headers = HeadersBuilder().build(),
            varyKeys = emptyMap(),
            body = readResourceImage("placeholder.png")
        )
        try {
            runBlocking {
                (0 until 100).forEach { _ ->
                    withContext(Dispatchers.IO) {
                        cacheTestImage(url, data)
                    }
                }

            }
        } catch (t: Throwable) {
            fail("cache failed", t)
        }
    }

    @Test
    fun concurrencyRWTest() {
        val url = Url("https://www.test.com/test.jpg")
        val data = CachedResponseData(
            url,
            HttpStatusCode.OK,
            GMTDate(Clock.System.now().toEpochMilliseconds() - 4000L),
            GMTDate(Clock.System.now().toEpochMilliseconds() - 3000L),
            version = HttpProtocolVersion.HTTP_2_0,
            expires = GMTDate(Clock.System.now().toEpochMilliseconds() + 24 * 3600 * 1000L),
            headers = HeadersBuilder().build(),
            varyKeys = emptyMap(),
            body = readResourceImage("placeholder.png")
        )
        try {
            runBlocking {
                cacheTestImage(url, data)
                (0 until 100).forEach {
                    async {
                        withContext(Dispatchers.IO) {
                            cacheTestImage(url, data)
                        }
                    }
                    async {
                        withContext(Dispatchers.IO) {
                            val byteArray = storage.find(url, emptyMap())!!.body
                            assertTrue(
                                byteArray.isNotEmpty(),
                                "The size of byte array should not empty"
                            )
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            fail("concurrency rw failed", t)
        }

    }


    private suspend fun cacheTestImage(url: Url, data: CachedResponseData) {
        storage.store(
            url, data
        )
    }


}