package top.heiha.huntun.hmage.cache

import com.benasher44.uuid.uuid4
import com.heiha.huntun.hog.logd
import com.heiha.huntun.hog.logw
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.http.HeadersImpl
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import top.heiha.huntun.hmage.TAG
import top.heiha.huntun.hmage.utils.isDebug
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class PersistentCacheStorage(
    maxCacheSize: Long,
    private val fileSystem: FileSystem,
    cachePath: Path
) : CacheStorage {
    private val networkCacheDirectory = cachePath.div("network_cache")
    private var isLruInitialized = false
    private val lruInitLock = Mutex()
    private val lru = Lru<String, Unit>(Long.MAX_VALUE)
    private val lruPath = "$cachePath${Path.DIRECTORY_SEPARATOR}lru".toPath()
    private val locksLock = Mutex()
    private val readLocks = mutableMapOf<String, Mutex>()
    private val writeLocks = mutableMapOf<String, Mutex>()
    private val fileSystemWriteLock = Mutex()

    @Transient
    private var rc = 0

    @Transient
    private var maxCacheSize = maxCacheSize
    private val lruChannel =
        Channel<Unit>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        createDirectoriesIfNotExists(lruPath.parent!!)
        createDirectoriesIfNotExists(networkCacheDirectory)
        GlobalScope.launch {
            for (u in lruChannel) {
                fileSystemWriteLock.withLock {
                    getLru().persistent()
                }
            }
        }
    }

    private suspend fun getReadLock(key: String): Mutex {
        return locksLock.withLock {
            readLocks.getOrPut(key) {
                Mutex()
            }
        }
    }

    private suspend fun getWriteLock(key: String): Mutex {
        return locksLock.withLock {
            writeLocks.getOrPut(key) {
                Mutex()
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

    class LockOwnerElement(val tag: String) : AbstractCoroutineContextElement(LockOwnerElement) {
        companion object : CoroutineContext.Key<LockOwnerElement> {}
    }

    @Transient
    private var gateMan: String? = null

    override suspend fun findAll(url: Url): Set<CachedResponseData> =
        withLockTag("findAll") {
            val lockTag = coroutineContext[LockOwnerElement]?.tag
            val key = url.toKey()
            val readLock = getReadLock(key)
            val writeLock = getWriteLock(key)
            readLock.withLock(lockTag) {
                if (rc == 0) {
                    gateMan = lockTag
                    writeLock.lock(gateMan)
                }
                rc++
            }

            // read
            val result = try {
                val cacheFilePath = generateCachePath(url)
                if (fileSystem.exists(cacheFilePath)) {
                    updateLastAccessTime(url, false)
                    fileSystemWriteLock.withLock(lockTag) {
                        fileSystem.read(cacheFilePath) {
                            readUtf8()
                        }.let { jsonString ->
                            try {
                                val result =
                                    Json.decodeFromString<PersistentResponseData>(jsonString)
                                        .toCache()
                                result
                            } catch (tr: Throwable) {
                                logw(TAG, "load local chache failed", tr)
                                null
                            }
                        }?.let {
                            setOf(it)
                            // update last access time
                        } ?: emptySet()
                    }
                } else {
                    emptySet()
                }
            } finally {
                readLock.withLock(lockTag) {
                    rc--
                    if (rc == 0) {
                        writeLock.unlock(gateMan)
                    }
                }
            }

            return@withLockTag result
        }


    override suspend fun store(url: Url, data: CachedResponseData) {
        withLockTag("store") {
            val lockTag = coroutineContext[LockOwnerElement]?.tag
            val key = url.toKey()
            val writeLock = getWriteLock(key)
            writeLock.withLock(lockTag) {
                updateLastAccessTime(url, true)

                fileSystemWriteLock.withLock(lockTag) {
                    fileSystem.write(generateCachePath(url)) {
                        writeUtf8(Json.encodeToString(data.toPersistent()))
                    }
                }
            }


            trim()
        }
    }

    internal fun generateCachePath(url: Url): Path {
        return networkCacheDirectory.resolve(
            "${url.hashCode()}.json"
        )
    }

    internal suspend fun clean() {
        fileSystemWriteLock.withLock {
            fileSystem.deleteRecursively(networkCacheDirectory, false)
        }
    }

    /**
     * Only for init phrase
     *
     * @param path
     */
    private fun createDirectoriesIfNotExists(path: Path) {
        if (fileSystem.exists(path).not()) {
            fileSystem.createDirectories(path)
        }
    }

    private suspend fun Mutex?.tryWithLock(block: () -> Unit) {
        if (this == null) {
            block()
        } else {
            this.withLock {
                block()
            }
        }
    }

    private suspend fun updateLastAccessTime(url: Url, write: Boolean) {
        val lru = getLru()
        if (write) {
            lru[url.toKey()] = Unit
        } else {
            lru[url.toKey()]
        }
        lru.submit()
    }

    private suspend fun trim() {
        val lockTag = coroutineContext[LockOwnerElement]?.tag
        // 71467
        var totalSize = 0L
        var flag = false
        getLru().asIterable().reversed().forEach {
            val file = it.first.toCacheFilePath()
            val lock = getWriteLock(it.first)
            lock.withLock(lockTag) {
                fileSystemWriteLock.withLock(lockTag) {
                    if (flag.not()) {
                        val fileSize = fileSystem.metadata(file).size ?: 0
                        if (totalSize + fileSize > maxCacheSize) {
                            flag = true
                        } else {
                            totalSize += fileSize
                        }
                    }
                    if (flag) {
                        getLru() -= it.first
                        fileSystem.delete(file)
                    }
                }
            }

        }
    }

    suspend fun updateMaxCacheSize(maxCacheSize: Long) {
        withLockTag("updateMaxCacheSize") {
            this@PersistentCacheStorage.maxCacheSize = maxCacheSize
            trim()
        }
    }


    private fun Url.toKey() = hashCode().toString()

    private suspend fun getLru(): Lru<String, Unit> {
        val lockTag = coroutineContext[LockOwnerElement]?.tag
        if (isLruInitialized.not()) {
            lruInitLock.withLock(lockTag) {
                if (isLruInitialized.not()) {
                    isLruInitialized = true
                    if (fileSystem.exists(lruPath)) {
                        fileSystem.read(lruPath) {
                            readUtf8().split("\n").forEach {
                                lru[it] = Unit
                            }
                        }
                    }

                }
            }
        }
        return lru
    }

    private suspend fun Lru<String, Unit>.submit() {
        lruChannel.send(Unit)
    }

    private suspend fun Lru<String, Unit>.persistent() {
        fileSystem.write(lruPath) {
            val lruString = this@persistent.asIterable().joinToString("\n")
            write(lruString.encodeUtf8())
        }
    }

    private fun String.toCacheFilePath() =
        "$networkCacheDirectory${Path.DIRECTORY_SEPARATOR}$this.json".toPath()

    private suspend fun <R> withLockTag(tag: String, block: suspend () -> R): R = if (isDebug) {
        withContext(LockOwnerElement("$tag#${uuid4()}")) {
            block()
        }
    } else {
        block()
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
