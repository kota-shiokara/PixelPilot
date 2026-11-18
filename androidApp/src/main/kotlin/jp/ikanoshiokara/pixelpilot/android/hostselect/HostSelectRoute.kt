package jp.ikanoshiokara.pixelpilot.android.hostselect

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import jp.ikanoshiokara.pixelpilot.android.R
import jp.ikanoshiokara.pixelpilot.model.connection.HostDevice
import jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid.Discoverable
import org.koin.androidx.compose.koinViewModel

/** 実行時に求める権限(Android 12 以降)。11 以前はインストール時に許可されるため、何も求めない。 */
private val bluetoothPermissions: Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
    } else {
        emptyArray()
    }

/** 非対応端末の報告先(GitHub の issue の作成画面)。 */
private const val REPORT_URL = "https://github.com/kota-shiokara/PixelPilot/issues/new"

/**
 * ホスト選択画面に、Activity が要る操作(権限のダイアログ、Bluetooth の有効化、検出可能化、外部の画面を開く)をつなぐ。
 *
 * Bluetooth の有効化と検出可能化の Intent は、Android 12 以降はそれぞれ `BLUETOOTH_CONNECT` / `BLUETOOTH_ADVERTISE`
 * の許可が前提。権限が必要な状態(NeedsPermission)ではこれらのボタンを出さないため、MissingPermission を抑止している。
 */
@SuppressLint("MissingPermission")
@Composable
fun HostSelectRoute(viewModel: HostSelectViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val deviceInfo = stringResource(R.string.unsupported_device, Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE)

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.all { it }) {
            viewModel.onPermissionGranted()
        } else {
            // 拒否された直後に説明を出せない(false)なら、「今後表示しない」になっている
            val activity = context.findActivity()
            val blocked = bluetoothPermissions.any { !ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
            viewModel.onPermissionDenied(blocked)
        }
    }
    // Bluetooth がオンになったことは、BluetoothHidController が Bluetooth の状態の通知で受け取る
    val enableBluetoothLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val discoverableLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // 許可されると、結果のコードに検出可能な秒数が入る(拒否されると RESULT_CANCELED = 0)
        if (result.resultCode > 0) viewModel.onDiscoverableStarted(result.resultCode)
    }

    LaunchedEffect(Unit) { viewModel.refreshPairedHosts() }

    val actions = remember(viewModel, context) {
        object : HostSelectActions {
            override fun requestPermission() {
                permissionLauncher.launch(bluetoothPermissions)
            }

            override fun openAppSettings() {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            }

            override fun enableBluetooth() {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }

            override fun reportUnsupported() {
                val uri = Uri.parse(REPORT_URL).buildUpon()
                    .appendQueryParameter("title", "Bluetooth HID 非対応の端末: ${Build.MANUFACTURER} ${Build.MODEL}")
                    .appendQueryParameter("body", deviceInfo)
                    .build()
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: ActivityNotFoundException) {
                    // ブラウザが無い端末では何もしない
                }
            }

            override fun connect(host: HostDevice) = viewModel.connect(host)

            override fun retry() = viewModel.retry()

            override fun startPairing() {
                discoverableLauncher.launch(Discoverable.requestIntent())
            }

            override fun cancelPairing() = viewModel.stopPairing()

            override fun disconnect() = viewModel.disconnect()
        }
    }

    HostSelectScreen(state = state, deviceInfo = deviceInfo, actions = actions)
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("Activity が見つからない")
}
