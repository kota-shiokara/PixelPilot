package view.screen.qr

import androidx.compose.ui.graphics.painter.Painter
import domain.model.Resources
import domain.usecase.GetAddressUseCase
import domain.usecase.QrGenerateUseCase
import view.screen.qr.QrScreenState.SessionState
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
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket

class QrScreenStateHolder(
    val qrGenerateUseCase: QrGenerateUseCase,
    val getAddressUseCase: GetAddressUseCase,
): ChildStateHolder<QrScreenState> {
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + Dispatchers.IO)

    private val qrState: MutableStateFlow<Painter?> = MutableStateFlow(null)
    private val sessionState: MutableStateFlow<SessionState> = MutableStateFlow(SessionState.WAIT)
    override val state: StateFlow<QrScreenState> = combine(
        qrState, sessionState
    ) { qrState, sessionState ->
        QrScreenState(qrState, sessionState)
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), QrScreenState())

    private val serverSocket = ServerSocket(Resources.SERVER_PORT)

    override fun setup() {
        val address = getAddressUseCase()
        println(address)
        qrState.value = qrGenerateUseCase(address)
    }

    override fun dispose() {
        coroutineScope.cancel()
    }

    fun waitClient() {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val socket = serverSocket.accept()
                    val inputStream = DataInputStream(socket.getInputStream())
                    val message = inputStream.readUTF()
                    println(message)

                    if (message == "ping") {
                        val outputStream = DataOutputStream(socket.getOutputStream())
                        outputStream.writeUTF("pong")
                        sessionState.value = SessionState.CONNECT
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}