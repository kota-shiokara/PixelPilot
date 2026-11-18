package jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid

import android.bluetooth.BluetoothAdapter
import android.content.Intent

/**
 * 新しいホストとペアリングするために、この端末を他の機器から見つけられるようにする(検出可能にする)。
 *
 * [requestIntent] を Activity から起動すると、システムの確認ダイアログが出る。許可されると
 * [DURATION_SECONDS] 秒間、PC の Bluetooth 設定の一覧にこの端末が表示される。
 * Android 12 以降は `BLUETOOTH_ADVERTISE` の許可が必要。
 */
object Discoverable {
    /** 検出可能にする時間(docs/design/v2.0-standard.md 13. 決定事項 14)。 */
    const val DURATION_SECONDS = 120

    fun requestIntent(): Intent =
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DURATION_SECONDS)
}
