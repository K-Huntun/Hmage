# Hmage - Image Loader for Compose Multiple Platforms

Hmage is a Compose-based image loading library that supports multiple platforms, including Android and iOS. It provides an easy-to-use interface for loading and displaying remote images.

## Installation

Add the following dependency to your project's build.gradle file:

```groovy
dependencies {
    implementation 'top.heiha.huntun:hmage:0.0.1-dev'
}
```

## Usage

Hmage provides two composable functions for loading and displaying images:

```kotlin
@Composable
fun Hmage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
)

@Composable
fun Hmage(
    source: StatedImageSource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
)
```

The first function takes a URL string as the image source, while the second function takes a `StatedImageSource` object that represents the image source, including the normal, placeholder, and error states.

```kotlin
data class StatedImageSource(
    val normal: String,
    val placeholder: String? = null,
    val error: String? = null
)
```
### Cache
Hmage uses a memory & disk cache to cache the loaded images, so subsequent requests for the same image will not trigger another network request.
- memory cache: Use LRU Strategy
- disk cache: Just cache, the particular strategy will be implemented in future.


## Example

Here is an example of how to use Hmage to load an image from a remote URL:

```kotlin
Hmage(
    url = "https://example.com/image.png",
    contentDescription = "Example Image",
    modifier = Modifier.fillMaxSize(),
    contentScale = ContentScale.FillWidth
)
```

And here is an example of how to use Hmage to load an image from a local resource:

```kotlin
Hmage(
    source = StatedImageSource(
        normal = "image.png",  // expect image
        placeholder = "placeholder.png", // display before "normal" be loaded
        error = "error.png" // display after "normal" be loaded failed
    ),
    contentDescription = "Example Image",
    modifier = Modifier.fillMaxSize(),
    contentScale = ContentScale.FillWidth
)
```

## License

Hmage is released under the MIT License. See [LICENSE](LICENSE) for details.

## References
The images are used in demo are from [flaticon](https://www.flaticon.com)

- [people stickers](https://www.flaticon.com/free-stickers/people)
- [Image Comics icons](https://www.flaticon.com/free-icons/image-comics)
- 