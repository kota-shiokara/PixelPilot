package jp.ikanoshiokara.pixelpilot.domain.transport

import jp.ikanoshiokara.pixelpilot.model.connection.TransportKind
import kotlinx.coroutines.flow.StateFlow

/**
 * この端末で使える経路。プラットフォームごとに実装し、各アプリが Koin で注入する。
 *
 * 共通コードは OS 名で分岐せず、ここで分かる経路で振る舞いを決める(docs/design/v2.0.md 7.)。
 * 例: Android でも HID Device プロファイルが無効化された端末では [TransportKind.BLUETOOTH_HID] を含まない。
 */
interface PlatformTransports {
    /**
     * 使える経路の一覧。判定に時間がかかる経路(HID Device プロファイルの取得など)は、
     * 確認が済んだ時点で一覧が変わる。
     */
    val available: StateFlow<List<TransportKind>>
}

/** デスクトップ版が必須かどうか。PC に直接つながる経路(Bluetooth HID)が無ければ必須。 */
val List<TransportKind>.requiresDesktop: Boolean
    get() = TransportKind.BLUETOOTH_HID !in this
