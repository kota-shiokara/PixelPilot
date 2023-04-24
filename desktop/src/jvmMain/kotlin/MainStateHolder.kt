import domain.model.Resources.Companion.SERVER_PORT
import domain.model.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import view.common.ChildStateHolder
import view.screen.qr.QrScreenStateHolder
import java.net.ServerSocket

class MainStateHolder(
    val qrScreenStateHolder: QrScreenStateHolder
) {
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + Dispatchers.IO)
    private val currentScreen: MutableStateFlow<Screen> = MutableStateFlow(Screen.QrScreen)
    private val isDark: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val state: StateFlow<MainState> = combine(
        currentScreen,
        isDark
    ) { currentScreen, isDark ->
        MainState(currentScreen, isDark)
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), MainState())

    private val children: List<ChildStateHolder<*>> = listOf(
        qrScreenStateHolder
    )
    val serverSocket = ServerSocket(SERVER_PORT)

    fun setup() {
        children.forEach { it.setup() }
    }

    fun dispose() {
        children.forEach { it.dispose() }
    }
}