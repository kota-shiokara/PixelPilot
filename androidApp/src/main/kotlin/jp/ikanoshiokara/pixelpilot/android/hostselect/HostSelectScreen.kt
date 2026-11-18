package jp.ikanoshiokara.pixelpilot.android.hostselect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.ikanoshiokara.pixelpilot.android.R
import jp.ikanoshiokara.pixelpilot.model.connection.ErrorReason
import jp.ikanoshiokara.pixelpilot.model.connection.HostDevice
import jp.ikanoshiokara.pixelpilot.view.common.PixelPilotColors

/** ホスト選択画面の操作。Activity が要るもの(ダイアログの起動など)は呼び出し側が実装する。 */
interface HostSelectActions {
    fun requestPermission()
    fun openAppSettings()
    fun enableBluetooth()
    fun reportUnsupported()
    fun connect(host: HostDevice)
    fun retry()
    fun startPairing()
    fun cancelPairing()
    fun disconnect()
}

/**
 * ホスト選択画面(画面デザインの仮案 1・1-a〜1-g)。状態を描画し、操作を [actions] に渡すだけにする。
 * v2.0 は「Bluetooth で接続」のカードのみ。
 */
@Composable
fun HostSelectScreen(state: HostSelectUiState, deviceInfo: String, actions: HostSelectActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.host_select_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        when (state) {
            HostSelectUiState.Checking -> CheckingCard()
            is HostSelectUiState.NeedsPermission -> PermissionCard(state.blocked, actions)
            HostSelectUiState.BluetoothOff -> MessageCard(
                badge = Badge.ActionNeeded,
                title = stringResource(R.string.bluetooth_off_title),
                body = stringResource(R.string.bluetooth_off_body),
                buttonLabel = stringResource(R.string.bluetooth_off_button),
                onClick = actions::enableBluetooth,
            )
            HostSelectUiState.Unsupported -> UnsupportedCard(deviceInfo, actions)
            is HostSelectUiState.Hosts -> HostsContent(state, actions)
            is HostSelectUiState.Connected -> ConnectedCard(state.host, actions)
        }
    }
}

private enum class Badge(val color: Color, val label: Int) {
    ActionNeeded(PixelPilotColors.StatusActionNeeded, R.string.badge_action_needed),
    Unsupported(PixelPilotColors.StatusError, R.string.badge_unsupported),
    Checking(PixelPilotColors.OnSurfaceMuted, R.string.badge_checking),
    Discoverable(PixelPilotColors.Accent, R.string.badge_discoverable),
    Expired(PixelPilotColors.StatusActionNeeded, R.string.badge_expired),
    Connected(PixelPilotColors.StatusConnected, R.string.connected_status),
}

/** 「Bluetooth で接続」のカードの枠。見出しと、状態を示す色の丸と文言を出す。 */
@Composable
private fun BluetoothCard(
    badge: Badge?,
    title: String,
    borderColor: Color = PixelPilotColors.Outline,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.bluetooth_section),
            style = MaterialTheme.typography.labelLarge,
            color = PixelPilotColors.OnSurfaceMuted,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelPilotColors.Surface, RoundedCornerShape(18.dp))
                .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (badge != null) StatusBadge(badge)
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun StatusBadge(badge: Badge) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).background(badge.color, CircleShape))
        Text(text = stringResource(badge.label), color = badge.color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Body(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = PixelPilotColors.OnSurfaceMuted)
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(text = label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(text = label, color = PixelPilotColors.OnSurface)
    }
}

@Composable
private fun MessageCard(badge: Badge, title: String, body: String, buttonLabel: String, onClick: () -> Unit) {
    BluetoothCard(badge = badge, title = title) {
        Body(body)
        PrimaryButton(buttonLabel, onClick)
    }
}

@Composable
private fun CheckingCard() {
    BluetoothCard(badge = Badge.Checking, title = stringResource(R.string.checking_title)) {
        Body(stringResource(R.string.checking_body))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PermissionCard(blocked: Boolean, actions: HostSelectActions) {
    if (blocked) {
        MessageCard(
            badge = Badge.ActionNeeded,
            title = stringResource(R.string.permission_blocked_title),
            body = stringResource(R.string.permission_blocked_body),
            buttonLabel = stringResource(R.string.permission_blocked_button),
            onClick = actions::openAppSettings,
        )
    } else {
        MessageCard(
            badge = Badge.ActionNeeded,
            title = stringResource(R.string.permission_title),
            body = stringResource(R.string.permission_body),
            buttonLabel = stringResource(R.string.permission_button),
            onClick = actions::requestPermission,
        )
    }
}

@Composable
private fun UnsupportedCard(deviceInfo: String, actions: HostSelectActions) {
    BluetoothCard(
        badge = Badge.Unsupported,
        title = stringResource(R.string.unsupported_title),
        borderColor = PixelPilotColors.StatusError,
    ) {
        Body(stringResource(R.string.unsupported_body))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelPilotColors.Background, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = deviceInfo, fontSize = 12.sp, color = PixelPilotColors.OnSurfaceMuted)
            TextButton(onClick = actions::reportUnsupported) {
                Text(text = stringResource(R.string.unsupported_report))
            }
        }
    }
}

@Composable
private fun HostsContent(state: HostSelectUiState.Hosts, actions: HostSelectActions) {
    when (val pairing = state.pairing) {
        is PairingState.Discoverable -> PairingCard(pairing, actions)
        PairingState.Expired -> MessageCard(
            badge = Badge.Expired,
            title = stringResource(R.string.pairing_expired_title),
            body = stringResource(R.string.pairing_expired_body),
            buttonLabel = stringResource(R.string.pairing_again),
            onClick = actions::startPairing,
        )
        PairingState.Idle -> Unit
    }

    if (state.error != null) ErrorCard(state.error, actions)

    if (state.lastHost != null) {
        Section(stringResource(R.string.last_host)) {
            HostRow(state.lastHost, connecting = state.connecting?.address == state.lastHost.address, actions)
        }
    }

    Section(stringResource(R.string.paired_hosts)) {
        if (state.otherHosts.isEmpty()) {
            Body(stringResource(R.string.paired_hosts_empty))
        } else {
            state.otherHosts.forEachIndexed { index, host ->
                if (index > 0) HorizontalDivider(color = PixelPilotColors.Background)
                HostRow(host, connecting = state.connecting?.address == host.address, actions)
            }
        }
    }
    Body(stringResource(R.string.paired_hosts_note))

    if (state.pairing == PairingState.Idle) {
        Spacer(modifier = Modifier.height(8.dp))
        SecondaryButton(stringResource(R.string.new_pairing), actions::startPairing)
        Body(stringResource(R.string.new_pairing_note))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = PixelPilotColors.OnSurfaceMuted,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelPilotColors.Surface, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun HostRow(host: HostDevice, connecting: Boolean, actions: HostSelectActions) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = host.name ?: stringResource(R.string.unnamed_host, host.address),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (connecting) {
            Text(text = stringResource(R.string.connecting), color = PixelPilotColors.OnSurfaceMuted)
        } else {
            Button(onClick = { actions.connect(host) }) {
                Text(text = stringResource(R.string.connect))
            }
        }
    }
}

@Composable
private fun PairingCard(pairing: PairingState.Discoverable, actions: HostSelectActions) {
    BluetoothCard(
        badge = Badge.Discoverable,
        title = stringResource(R.string.pairing_title),
        borderColor = PixelPilotColors.Accent,
    ) {
        Body(stringResource(R.string.pairing_steps))
        LinearProgressIndicator(
            progress = { pairing.remainingSeconds.toFloat() / pairing.totalSeconds },
            modifier = Modifier.fillMaxWidth(),
        )
        Body(
            stringResource(
                R.string.pairing_remaining,
                pairing.remainingSeconds / 60,
                pairing.remainingSeconds % 60,
                pairing.totalSeconds,
            ),
        )
        SecondaryButton(stringResource(R.string.pairing_cancel), actions::cancelPairing)
    }
}

@Composable
private fun ErrorCard(reason: ErrorReason, actions: HostSelectActions) {
    val message = when (reason) {
        ErrorReason.CONNECTION_LOST -> R.string.error_connection_lost
        ErrorReason.HOST_NOT_FOUND -> R.string.error_host_not_found
        ErrorReason.HOST_REJECTED -> R.string.error_host_rejected
        ErrorReason.REGISTER_FAILED -> R.string.error_register_failed
        // ホスト選択画面では、Bluetooth オフは案内(BluetoothOff)として出すため、ここには来ない
        ErrorReason.BLUETOOTH_OFF -> R.string.bluetooth_off_title
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixelPilotColors.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, PixelPilotColors.StatusError, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.size(8.dp).background(PixelPilotColors.StatusError, CircleShape))
        Text(text = stringResource(message), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        // 再接続中(CONNECTION_LOST)は自動で試しているため、ボタンは出さない
        if (reason != ErrorReason.CONNECTION_LOST) {
            TextButton(onClick = actions::retry) { Text(text = stringResource(R.string.retry)) }
        }
    }
}

/** 接続できた(メイン画面は v2.0.1 の #45 で追加する。それまでの仮の表示)。 */
@Composable
private fun ConnectedCard(host: HostDevice, actions: HostSelectActions) {
    BluetoothCard(
        badge = Badge.Connected,
        title = host.name ?: stringResource(R.string.unnamed_host, host.address),
        borderColor = PixelPilotColors.StatusConnected,
    ) {
        Body(stringResource(R.string.connected_note))
        SecondaryButton(stringResource(R.string.disconnect), actions::disconnect)
    }
}
