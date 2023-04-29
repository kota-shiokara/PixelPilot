import androidx.compose.ui.graphics.painter.Painter
import domain.model.Screen

data class MainState(
//    val currentScreen: Screen? = null,
//    val isDark: Boolean = false,
    val qrcode: Painter? = null,
    val session: Session = Session.WAIT,
    val receive: String = ""
) {
    sealed class Session {
        data object WAIT: Session()
        data object CONNECT: Session()
    }
}
