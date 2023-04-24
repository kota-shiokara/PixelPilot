import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import domain.di.domainModule
import domain.model.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import view.di.stateHolderModule
import view.screen.qr.QrScreen
import java.io.DataInputStream

fun main() {
    startKoin {
        modules(domainModule, stateHolderModule)
    }

    application {
        val mainStateHolder by remember { mutableStateOf(GlobalContext.get().get<MainStateHolder>()) }
        val state by mainStateHolder.state.collectAsState()

        MaterialTheme {
            Window(
                title = "PixelPilot",
                onCloseRequest = ::exitApplication,
            ) {
                DisposableEffect(mainStateHolder) {
                    mainStateHolder.setup()
                    onDispose {
//                        val frameWindowScope = this@Window
//                        mainStateHolder.saveSetting(frameWindowScope.getWindowSize())
                        mainStateHolder.dispose()
                    }
                }

                Surface {
                    val composableScope = rememberCoroutineScope()

                    composableScope.launch(Dispatchers.IO) {
                        try {
                            while (true) {
                                val socket = mainStateHolder.serverSocket.accept()
                                val inputStream = DataInputStream(socket.getInputStream())
                                val message = inputStream.readUTF()
                                println("receive: $message")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    when(state.currentScreen) {
                        is Screen.QrScreen -> {
                            val qrScreenStateHolder = mainStateHolder.qrScreenStateHolder
                            QrScreen(qrScreenStateHolder)
                        }
                        is Screen.SessionScreen -> {

                        }
                        else -> {

                        }
                    }
                }
            }
        }
    }
}
