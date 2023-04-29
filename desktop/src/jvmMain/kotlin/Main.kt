import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import domain.di.domainModule
import domain.di.repositoryModule
import domain.model.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import view.di.stateHolderModule
import view.screen.qr.QrContent
import view.screen.qr.QrScreen
import java.io.DataInputStream

fun main() {
    startKoin {
        modules(domainModule, stateHolderModule, repositoryModule)
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

                when(state.session) {
                    is MainState.Session.WAIT -> mainStateHolder.waitClient()
                    is MainState.Session.CONNECT -> mainStateHolder.waitCommand()
                }

                Surface {
                    QrContent(
                        state.qrcode,
                        state.session.toString()
                    )
//                    when(state.currentScreen) {
//                        is Screen.QrScreen -> {
//                            val qrScreenStateHolder = mainStateHolder.qrScreenStateHolder
//                            QrScreen(qrScreenStateHolder)
//                        }
//                        is Screen.SessionScreen -> {
//
//                        }
//                        else -> {
//
//                        }
//                    }
                }
            }
        }
    }
}
