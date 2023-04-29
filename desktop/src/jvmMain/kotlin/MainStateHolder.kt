import androidx.compose.ui.graphics.painter.Painter
import domain.model.Screen
import domain.usecase.GetAddressUseCase
import domain.usecase.QrGenerateUseCase
import MainState.Session
import domain.model.Resources
import domain.usecase.KeyEventUseCase
import jp.ikanoshiokara.common.domain.model.command.KeyCode
import jp.ikanoshiokara.common.domain.model.command.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import view.common.ChildStateHolder
import view.screen.qr.QrScreenState
import view.screen.qr.QrScreenStateHolder
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket

class MainStateHolder(
//    val qrScreenStateHolder: QrScreenStateHolder,
    val qrGenerateUseCase: QrGenerateUseCase,
    val getAddressUseCase: GetAddressUseCase,
    val keyEventUseCase: KeyEventUseCase
) {
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + Dispatchers.IO)
//    private val currentScreen: MutableStateFlow<Screen> = MutableStateFlow(Screen.QrScreen)
//    private val isDark: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val qrState: MutableStateFlow<Painter?> = MutableStateFlow(null)
    private val sessionState: MutableStateFlow<Session> = MutableStateFlow(Session.WAIT)
    private val serverSocket = ServerSocket(Resources.SERVER_PORT)

    val state: StateFlow<MainState> = combine(
        qrState,
        sessionState
    ) { qrState, sessionState ->
        MainState(qrState, sessionState)
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), MainState())

//    private val children: List<ChildStateHolder<*>> = listOf(
//        qrScreenStateHolder
//    )

    fun setup() {
        val address = getAddressUseCase()
        println(address)
        qrState.value = qrGenerateUseCase(address)
//        children.forEach { it.setup() }
    }

    fun dispose() {
//        children.forEach { it.dispose() }
    }

    fun waitClient() {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    while (true) {
                        val socket = serverSocket.accept()
                        val inputStream = DataInputStream(socket.getInputStream())
                        val message = inputStream.readUTF()
                        println(message)

                        if (message == "ping") {
                            val outputStream = DataOutputStream(socket.getOutputStream())
                            outputStream.writeUTF("pong")
                            sessionState.value = Session.CONNECT
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun waitCommand() {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    while (true) {
                        val socket = serverSocket.accept()
                        val inputStream = DataInputStream(socket.getInputStream())
                        val message = inputStream.readUTF()
                        println(message)
                        receivedCommand(message)

                        if (message == "exit") {
                            sessionState.value = Session.WAIT
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun receivedCommand(message: String) {
        val command = message.toKeyEvent()
        keyEventUseCase(command)
    }
}

fun String.toKeyEvent(): KeyEvent {
    val tmp = this.split(",")
    if (tmp.size != 2) {
        return KeyEvent.Default
    }
    val event = tmp[0]
    val code = tmp[1]
    return if (event.isNotEmpty() && code.isNotEmpty()) {
        val keyCode: KeyCode = when(code.toInt()) {
            37 -> KeyCode.LEFT
            38 -> KeyCode.UP
            39 -> KeyCode.RIGHT
            40 -> KeyCode.DOWN
            else -> KeyCode.DEFAULT
        }
        when(event) {
            "keyPress" -> KeyEvent.KeyPress(keyCode)
            "keyRelease" -> KeyEvent.KeyRelease(keyCode)
            else -> KeyEvent.Default
        }
    } else {
        KeyEvent.Default
    }
}