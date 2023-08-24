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
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
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
    //    private val lruLock = Mutex()
//    private val readLock = Mutex()
//    private val writeLock = Mutex()
//    private val readLocks = mutableMapOf<String, AtomicRef<Boolean>>()
//    private val writeLocks = mutableMapOf<String, AtomicRef<Boolean>>()
    private val lru = linkedSetOf<String>()
    private var maxCacheSize = maxCacheSize

//    private suspend fun getReadLock(key: String): AtomicRef<Boolean> {
//        return readLock.withLock {
//            readLocks.getOrPut(key) {
//                AtomicRef(false)
//            }
//        }
//    }
//
//    private suspend fun getWriteLock(key: String): AtomicRef<Boolean> {
//        return writeLock.withLock {
//            writeLocks.getOrPut(key) {
//                AtomicRef(false)
//            }
//        }
//    }

    override suspend fun find(
        url: Url,
        varyKeys: Map<String, String>
    ): CachedResponseData? {
        logd(TAG, "find key: $url")
        // ignore varyKeys, just use url as key
        return findAll(url).firstOrNull()
    }

    override suspend fun findAll(url: Url): Set<CachedResponseData> {
        logd(TAG, "findAll key: $url, hashcode: ${url.hashCode()}")
        val cacheFilePath = fileSystem.listOrNull(networkCacheDirectory)?.find {
            it.name.startsWith(url.hashCode().toString())
        }
        return if (cacheFilePath != null) {
            fileSystem.read(cacheFilePath) {
                readUtf8()
            }.let { jsonString ->
                try {
                    val result = Json.decodeFromString<PersistentResponseData>(jsonString).toCache()
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
    }

    override suspend fun store(url: Url, data: CachedResponseData) {
        val key = url.hashCode().toString()
//        if (getWriteLock(key).getAndSet(true).not() && getReadLock(key).value.not()) {
        logd(TAG, "store cache, key: $url, hashcode: ${url.hashCode()}")
        createDirectoriesIfNotExists(networkCacheDirectory)
        fileSystem.write(generateCachePath(url)) {
            writeUtf8(Json.encodeToString(data.toPersistent()))
        }
        trim()
//        }
    }

    internal fun generateCachePath(url: Url): Path {
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
        fileSystem.listOrNull(networkCacheDirectory)?.sortedBy {
            it.name.substringBeforeLast(".").substringAfter("_").toLong()
        }?.let { sortedList ->
            var totalSize = fileSystem.metadata(networkCacheDirectory).size ?: 0
            val iterator = sortedList.iterator()
            while (totalSize > maxCacheSize) {
                if (iterator.hasNext()) {
                    val file = iterator.next()
                    totalSize -= fileSystem.metadata(file).size ?: 0
                    fileSystem.delete(file)
                }
            }
        }
    }

    private fun updateMaxCacheSize(maxCacheSize: Long) {
        this.maxCacheSize = maxCacheSize
        trim()
    }

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