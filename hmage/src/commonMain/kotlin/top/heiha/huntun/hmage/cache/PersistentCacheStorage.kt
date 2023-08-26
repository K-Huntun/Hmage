package top.heiha.huntun.hmage.cache

import com.heiha.huntun.hog.logd
import com.heiha.huntun.hog.logw
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.http.HeadersImpl
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import top.heiha.huntun.hmage.TAG

class PersistentCacheStorage(
    maxCacheSize: Long,
    private val fileSystem: FileSystem,
    private val networkCacheDirectory: Path
) : CacheStorage {
    private val locksLock = Mutex()
    private val readLocks = mutableMapOf<String, AtomicBoolean>()
    private val writeLocks = mutableMapOf<String, AtomicBoolean>()

    @Transient
    private var rc = 0
    private var maxCacheSize = maxCacheSize

    private suspend fun getReadLock(key: String): AtomicBoolean {
        return locksLock.withLock {
            readLocks.getOrPut(key) {
                atomic(false)
            }

        }
    }

    private suspend fun getWriteLock(key: String): AtomicBoolean {
        return locksLock.withLock {
            writeLocks.getOrPut(key) {
                atomic(false)
            }
        }
    }

    override suspend fun find(
        url: Url,
        varyKeys: Map<String, String>
    ): CachedResponseData? {
        logd(TAG, "find key: $url")
        // ignore varyKeys, just use url as key
        return findAll(url).firstOrNull()
    }

    override suspend fun findAll(url: Url): Set<CachedResponseData> {
        val key = url.toKey()
        val readLock = getReadLock(key)
        val writeLock = getWriteLock(key)
        while (readLock.compareAndSet(expect = false, update = true).not());
        if (rc == 0) {
            while (writeLock.compareAndSet(expect = false, update = true).not());
        }
        rc++
        readLock.value = false

        // read
        val result = try {
            logd(TAG, "findAll key: $url, hashcode: ${url.hashCode()}")
            val cacheFilePath = fileSystem.listOrNull(networkCacheDirectory)?.find {
                it.name.startsWith(url.hashCode().toString())
            }
            if (cacheFilePath != null) {
                fileSystem.read(cacheFilePath) {
                    readUtf8()
                }.let { jsonString ->
                    try {
                        val result =
                            Json.decodeFromString<PersistentResponseData>(jsonString).toCache()
                        updateLastAccessTime(url, cacheFilePath)
                        result
                    } catch (tr: Throwable) {
                        logw(TAG, "load local chache failed", tr)
                        null
                    }
                }?.let {
                    setOf(it)
                    // update last access time
                } ?: emptySet()
            } else {
                emptySet()
            }
        } finally {
            while (readLock.compareAndSet(expect = false, update = true).not());
            rc--
            if (rc == 0) {
                writeLock.value = false
            }
            readLock.value = false
        }

        return result
    }

    override suspend fun store(url: Url, data: CachedResponseData) {
        val key = url.toKey()
        val writeLock = getWriteLock(key)
        while (writeLock.compareAndSet(
                expect = false,
                update = true
            ).not()
        );
        try {
            logd(TAG, "store cache, key: $url, hashcode: ${url.hashCode()}")
            createDirectoriesIfNotExists(networkCacheDirectory)
            fileSystem.write(generateCachePath(url)) {
                writeUtf8(Json.encodeToString(data.toPersistent()))
            }
            trim()
        } finally {
            writeLock.value = false
        }
    }

    private fun generateCachePath(url: Url): Path {
        return networkCacheDirectory.resolve(
            "${url.hashCode()}_${
                Clock.System.now().toEpochMilliseconds()
            }.json"
        )
    }

    internal fun clean() {
        fileSystem.deleteRecursively(networkCacheDirectory, false)
    }

    private fun createDirectoriesIfNotExists(path: Path) {
        if (fileSystem.exists(path).not()) {
            fileSystem.createDirectories(path)
        }
    }

    private fun updateLastAccessTime(url: Url, path: Path) {
        fileSystem.atomicMove(path, generateCachePath(url))
    }

    private fun trim() {
        // 71467
        fileSystem.listOrNull(networkCacheDirectory)?.sortedByDescending {
            it.name.substringBeforeLast(".").substringAfter("_").toLong()
        }?.let { sortedList ->
            var totalSize = 0L
            var flag = false
            val iterator = sortedList.iterator()
            while (iterator.hasNext()) {
                val file = iterator.next()
                if (flag) {
                    fileSystem.delete(file)
                } else {
                    val fileSize = fileSystem.metadata(file).size ?: 0
                    logd(TAG, "fileSize: $fileSize")
                    if (totalSize + fileSize > maxCacheSize) {
                        fileSystem.delete(file)
                        flag = true
                    } else {
                        totalSize += fileSize
                    }
                }
            }
        }
    }

    fun updateMaxCacheSize(maxCacheSize: Long) {
        this.maxCacheSize = maxCacheSize
        trim()
    }

    private fun Url.toKey() = hashCode().toString()

}


internal fun CachedResponseData.toPersistent(): PersistentResponseData = PersistentResponseData(
    url = this.url.toString(),
    statusCode = PHttpStatusCode(this.statusCode.value, this.statusCode.description),
    requestTime = this.requestTime.timestamp,
    responseTime = this.responseTime.timestamp,
    version = this.version.toString(),
    expires = this.expires.timestamp,
    headers = this.headers.entries()
        .associateBy(keySelector = { it.key }, valueTransform = { it.value }),
    varyKeys = this.varyKeys,
    body = this.body
)

internal fun PersistentResponseData.toCache(): CachedResponseData = CachedResponseData(
    url = Url(this.url),
    statusCode = HttpStatusCode(this.statusCode.value, this.statusCode.description),
    requestTime = GMTDate(this.requestTime),
    responseTime = GMTDate(this.responseTime),
    version = HttpProtocolVersion.parse(this.version),
    expires = GMTDate(this.expires),
    headers = HeadersImpl(this.headers),
    varyKeys = this.varyKeys,
    body = this.body
)

@Serializable
data class PersistentResponseData(
    val url: String,
    val statusCode: PHttpStatusCode,
    val requestTime: Long,
    val responseTime: Long,
    val version: String,
    val expires: Long,
    val headers: Map<String, List<String>>,
    val varyKeys: Map<String, String>,
    val body: ByteArray
)

@Serializable
data class PHttpStatusCode(val value: Int, val description: String)