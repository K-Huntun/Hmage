package top.heiha.huntun.hmage.remote

import androidx.compose.ui.graphics.ImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import top.heiha.huntun.hmage.cache.PersistentCacheStorage
import top.heiha.huntun.hmage.cache.memoryCache
import top.heiha.huntun.hmage.utils.getAvailableCacheSpace
import top.heiha.huntun.hmage.utils.toImageBitmap

internal val httpClient by lazy {
    HttpClient {
        install(HttpCache) {
            publicStorage(PersistentCacheStorage(getAvailableCacheSpace()))
        }
    }
}

internal suspend fun HttpResponse.imageBitmap(originUrl: String? = null): ImageBitmap {
    if (status == HttpStatusCode.OK) {
        val byteArray: ByteArray = body()
        val bitmap = byteArray.toImageBitmap()
        return memoryCache.put(originUrl ?: this.request.url.toString(), bitmap)
    } else {
        throw ResponseException(this, "")
    }
}