package jp.ikanoshiokara.pixelpilot.android.main

import jp.ikanoshiokara.pixelpilot.model.connection.ConnectionState

/** アプリの画面(画面遷移は Android 固有の骨格、docs/design/v2.0-standard.md 9.)。 */
enum class AppScreen {
    /** ホスト選択(起動時の画面)。 */
    HOST_SELECT,

    /** メイン(マウス・キーボード・設定)。 */
    MAIN,
}

/**
 * 接続状態が変わったときに、どの画面を出すか。純 Kotlin の関数にして単体テストする。
 *
 * - 接続できたらメインへ進む
 * - メインにいる間は、切断・再接続中・エラーでもメインのまま(操作面の左上の表示で状態を伝える、設計書 4.3)
 * - 権限が無い・Bluetooth がオフ(接続前)・非対応のときは、案内のあるホスト選択へ戻す
 * - 接続先の選び直しはユーザーの操作で行う(この関数ではなく、画面側で HOST_SELECT にする)
 */
fun nextScreen(current: AppScreen, state: ConnectionState): AppScreen = when (state) {
    is ConnectionState.Connected -> AppScreen.MAIN
    ConnectionState.PermissionRequired,
    ConnectionState.BluetoothOff,
    ConnectionState.Unsupported,
    -> AppScreen.HOST_SELECT
    // 準備中・未接続・接続しようとしている・エラー: 今の画面のまま
    // (フォアグラウンドに戻ったときの再登録 → 自動接続の間に、ホスト選択へちらつかないように)
    ConnectionState.Registering,
    ConnectionState.Ready,
    is ConnectionState.Connecting,
    is ConnectionState.Error,
    -> current
}
