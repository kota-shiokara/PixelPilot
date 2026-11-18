package jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid

import android.content.Context
import android.content.pm.PackageManager
import jp.ikanoshiokara.pixelpilot.domain.transport.PlatformTransports
import jp.ikanoshiokara.pixelpilot.model.connection.TransportKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Android で使える経路。
 *
 * 起動直後は、端末が Bluetooth を備えていれば [TransportKind.BLUETOOTH_HID] を含める。
 * HID Device プロファイルが使えないと分かった時点(`getProfileProxy` の失敗やタイムアウト、#36)で、
 * [markUnavailable] によって一覧から外す。USB(AOA)は v2.x で追加する。
 */
class AndroidPlatformTransports(context: Context) : PlatformTransports {
    private val _available = MutableStateFlow(
        buildList {
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                add(TransportKind.BLUETOOTH_HID)
            }
        },
    )

    override val available: StateFlow<List<TransportKind>> = _available.asStateFlow()

    /** 経路が使えないと分かったときに、一覧から外す。 */
    internal fun markUnavailable(kind: TransportKind) {
        _available.update { it - kind }
    }
}
