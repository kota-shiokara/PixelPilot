package jp.ikanoshiokara.pixelpilot.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import jp.ikanoshiokara.pixelpilot.android.main.AppRoot
import jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid.BluetoothHidController
import jp.ikanoshiokara.pixelpilot.view.common.PixelPilotTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val bluetoothHid: BluetoothHidController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15(targetSdk 35)以降は画面の端まで描かれるため、古い OS でもそろえる。
        // v2.0 はダークテーマのみなので、ステータスバー・ナビゲーションバーのアイコンは常に明るい色にする
        val systemBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        enableEdgeToEdge(statusBarStyle = systemBarStyle, navigationBarStyle = systemBarStyle)
        super.onCreate(savedInstanceState)
        setContent {
            PixelPilotTheme {
                // 背景はシステムバーの裏まで塗り、中身はシステムバー・切り欠きを避けて置く
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                        AppRoot()
                    }
                }
            }
        }
    }

    /**
     * 画面が見えている間だけ Bluetooth HID を登録する(docs/design/v2.0-standard.md 7.3)。
     * 権限がまだ無い場合は、権限が必要な状態になる。設定画面から戻ってきたときも、ここで確認し直す。
     */
    override fun onStart() {
        super.onStart()
        bluetoothHid.start()
    }

    /** バックグラウンドに回ったら登録を解除する(接続の保持は v2.x の #54)。 */
    override fun onStop() {
        bluetoothHid.stop()
        super.onStop()
    }
}
