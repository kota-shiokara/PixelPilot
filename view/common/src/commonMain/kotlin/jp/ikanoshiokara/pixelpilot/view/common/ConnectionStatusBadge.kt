package jp.ikanoshiokara.pixelpilot.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.ikanoshiokara.pixelpilot.model.connection.ConnectionState
import jp.ikanoshiokara.pixelpilot.model.connection.ErrorReason
import jp.ikanoshiokara.pixelpilot.view.common.resources.Res
import jp.ikanoshiokara.pixelpilot.view.common.resources.error_bluetooth_off
import jp.ikanoshiokara.pixelpilot.view.common.resources.error_connection_lost
import jp.ikanoshiokara.pixelpilot.view.common.resources.error_host_not_found
import jp.ikanoshiokara.pixelpilot.view.common.resources.error_host_rejected
import jp.ikanoshiokara.pixelpilot.view.common.resources.error_register_failed
import jp.ikanoshiokara.pixelpilot.view.common.resources.status_connecting
import jp.ikanoshiokara.pixelpilot.view.common.resources.status_not_connected
import jp.ikanoshiokara.pixelpilot.view.common.resources.status_unnamed_host
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * 接続状態を、色の丸と文言で小さく示す(docs/design/v2.0-standard.md 4.3)。マウス画面の操作面の左上などに重ねて使う。
 *
 * - 接続中: 緑の丸 + 接続先の名前
 * - 切断・エラー: 赤の丸 + エラーの内容(7.5 の文言)。Bluetooth のオフは黄
 * - 色だけに頼らず、必ず文言を添える
 *
 * 重ねて置いてもタッチを受け取らない(操作面の上でもカーソルを動かせる)。
 */
@Composable
fun ConnectionStatusBadge(state: ConnectionState, modifier: Modifier = Modifier) {
    val (color, text) = when (state) {
        is ConnectionState.Connected ->
            PixelPilotColors.StatusConnected to (state.host.name ?: stringResource(Res.string.status_unnamed_host))
        is ConnectionState.Connecting -> PixelPilotColors.StatusActionNeeded to stringResource(Res.string.status_connecting)
        is ConnectionState.Error -> errorColor(state.reason) to stringResource(errorMessage(state.reason))
        else -> PixelPilotColors.OnSurfaceMuted to stringResource(Res.string.status_not_connected)
    }
    val background = if (state is ConnectionState.Error) Color(0xE63C1418) else Color(0xB8111418)
    Row(
        modifier = modifier
            .background(background, RoundedCornerShape(14.dp))
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(text = text, fontSize = 12.sp, color = PixelPilotColors.OnSurface)
    }
}

private fun errorColor(reason: ErrorReason): Color = when (reason) {
    ErrorReason.BLUETOOTH_OFF -> PixelPilotColors.StatusActionNeeded
    else -> PixelPilotColors.StatusError
}

private fun errorMessage(reason: ErrorReason): StringResource = when (reason) {
    ErrorReason.CONNECTION_LOST -> Res.string.error_connection_lost
    ErrorReason.BLUETOOTH_OFF -> Res.string.error_bluetooth_off
    ErrorReason.HOST_NOT_FOUND -> Res.string.error_host_not_found
    ErrorReason.HOST_REJECTED -> Res.string.error_host_rejected
    ErrorReason.REGISTER_FAILED -> Res.string.error_register_failed
}
