import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import domain.di.domainModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import view.di.stateHolderModule
import java.io.DataInputStream
import java.net.ServerSocket

private const val SERVER_PORT = 49152
@OptIn(ExperimentalFoundationApi::class)
fun main() {
    startKoin {
        modules(domainModule, stateHolderModule)
    }

    application {
        val mainStateHolder by remember { mutableStateOf(GlobalContext.get().get<MainStateHolder>()) }
        val qrCode by mainStateHolder.qrState.collectAsState()

        MaterialTheme {
            Window(
                title = "PixelPilot",
                onCloseRequest = ::exitApplication,
            ) {
                Surface {
                    val messages = mutableStateListOf<String>()
                    val composableScope = rememberCoroutineScope()

                    composableScope.launch(Dispatchers.IO) {
                        try {
                            val serverSocket = ServerSocket(SERVER_PORT)
                            while (true) {
                                val socket = serverSocket.accept()
                                val inputStream = DataInputStream(socket.getInputStream())
                                val message = inputStream.readUTF()
                                println("receive: $message")
                                messages.add(message)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    Image(
                        qrCode,
                        contentDescription = "",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
