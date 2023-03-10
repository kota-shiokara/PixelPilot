import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import jp.ikanoshiokara.common.App


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "PixelPilot"
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            App()
        }
    }
}
