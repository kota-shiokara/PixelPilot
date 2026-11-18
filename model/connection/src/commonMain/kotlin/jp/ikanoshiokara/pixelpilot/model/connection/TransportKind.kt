package jp.ikanoshiokara.pixelpilot.model.connection

/**
 * 入力を PC に届ける経路の種類。共通コードは OS 名ではなく、使える経路で振る舞いを決める。
 *
 * docs/design/v2.0.md 5. 接続方式、7. デスクトップ版が必須かどうかは利用可能な経路から導く
 */
enum class TransportKind {
    /** スマホが Bluetooth HID デバイスとして PC に直接つながる。デスクトップ版は不要。 */
    BLUETOOTH_HID,

    /** (v2.x)同じ LAN 内のデスクトップ版を経由する。 */
    AGENT_LAN,

    /** (v2.x)USB でつないだデスクトップ版を経由する。 */
    AGENT_USB,
}
