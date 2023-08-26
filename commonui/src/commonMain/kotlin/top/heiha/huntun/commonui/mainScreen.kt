package top.heiha.huntun.commonui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import top.heiha.huntun.hmage.Hmage
import top.heiha.huntun.hmage.StatedImageSource

@Composable
fun MainScreen() {
    RadioButtonSample()
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun RadioButtonSample() {
    val radioOptions = listOf(
        "Remote Image",
        "Remote Image with placeholder",
        "Remote Image with error",
        "Remote Image with placeholder & error"
    )
    var selectedIndex by remember { mutableStateOf(0) }
    var selectedText by remember { mutableStateOf(radioOptions[0]) }
    Column {
        radioOptions.forEachIndexed { index, text ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = index == selectedIndex,
                        onClick = {
                            selectedIndex = index
                            selectedText = text
                        }
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = index == selectedIndex,
                    onClick = {
                        selectedIndex = index
                        selectedText = text
                    }
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.merge(),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center ) {
            when (selectedIndex) {
                0 -> RemoteImage()
                1 -> RemoteImageWithPlaceholder()
                2 -> RemoteImageWithError()
                3 -> RemoteImageWithPlaceholderAndError()
            }
        }

    }
}

@Composable
fun RemoteImageWithPlaceholderAndError() {
    Hmage(
        StatedImageSource(
            "https://cdn-icons-png.flaticon.co5/4105446.png",
            placeholder = "placeholder.png",
            error = "error.png"
        ), contentDescription = ""
    )
}

//<a href="https://www.flaticon.com/free-stickers/people" title="people stickers">People stickers created by Stickers - Flaticon</a>
@Composable
fun RemoteImageWithError() {
    Hmage(
        StatedImageSource(
            "https://cdn-icons-png.flaticon.co/4105/4105450.png",
            error = "error.png"
        ), contentDescription = ""
    )
}

@Composable
fun RemoteImageWithPlaceholder() {
    Hmage(
        StatedImageSource(
            "https://cdn-icons-png.flaticon.com/512/4105/4105448.png",
            "placeholder.png" // <a href="https://www.flaticon.com/free-icons/image-comics" title="Image Comics icons">Image Comics icons created by Pop Vectors - Flaticon</a>
        ), contentDescription = ""
    )

}

@Composable
fun RemoteImage() {
    Hmage(
        "https://cdn-icons-png.flaticon.com/512/4105/4105446.png", // <a href="https://www.flaticon.com/free-stickers/people" title="people stickers">People stickers created by Stickers - Flaticon</a>
        contentDescription = ""
    )
}