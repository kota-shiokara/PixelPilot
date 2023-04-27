package view.screen.qr

import androidx.compose.ui.graphics.painter.Painter

data class QrScreenState(
    val qrcode: Painter? = null,
    val sessionType: SessionState = SessionState.WAIT
) {
    fun sessionConnectSuccess(): QrScreenState {
        return this.copy(sessionType = SessionState.CONNECT)
    }
    enum class SessionState {
        WAIT,
        CONNECT
    }
}
