package top.heiha.huntun.hmage

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.heiha.huntun.hog.loge
import io.ktor.client.request.get
import top.heiha.huntun.hmage.cache.memoryCache
import top.heiha.huntun.hmage.remote.httpClient
import top.heiha.huntun.hmage.remote.imageBitmap
import top.heiha.huntun.hmage.utils.readResourceImage
import top.heiha.huntun.hmage.utils.toImageBitmap

internal const val TAG = "Hmage"

@Composable
fun Hmage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) {
    Hmage(
        StatedImageSource(url),
        contentDescription,
        modifier,
        alignment,
        contentScale,
        alpha,
        colorFilter
    )
}

@Composable
fun Hmage(
    source: StatedImageSource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) {

    var imageBitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }

    LaunchedEffect(true) {
        val normal = source.normal
        val placeholder = source.placeholder
        if (placeholder != null) {
            imageBitmap = memoryCache.get(placeholder) ?: readResourceImage(placeholder).toImageBitmap()
        }

        imageBitmap =
           try {
                memoryCache.get(normal) ?: kotlin.run {
                    val response = httpClient.get(normal)
                    response.imageBitmap(normal)
                }
            } catch (tr: Throwable) {
                loge(TAG, "Load remote image failed", tr)
                val error = source.error
                if (error == null) {
                    null
                } else {
                    memoryCache.get(error) ?: readResourceImage(error).toImageBitmap()
                }
            }
    }

    imageBitmap?.let {
        Image(
            bitmap = it,
            contentDescription = contentDescription,
            modifier = modifier,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter
        )
    } ?: run {
        Box(modifier = modifier)
    }
}


/**
 * A model represent image source
 *
 * @property normal available type: url
 * @property placeholder local resource image name with image type suffix
 * @property error local resource image name with image type suffix
 */
data class StatedImageSource(
    val normal: String,
    val placeholder: String? = null,
    val error: String? = null
)

