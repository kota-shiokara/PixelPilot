package jp.ikanoshiokara.pixelpilot.model.input

/**
 * ホストから通知されたキーボードの LED の状態(キーボードの Output レポート、1 バイト)。
 * Caps Lock などの表示に使う。ビットの並びは Report Descriptor の LED の Usage(Num Lock〜Kana)のとおり。
 */
data class KeyboardLeds(val bits: Int = 0) {
    val numLock: Boolean get() = (bits and NUM_LOCK) != 0
    val capsLock: Boolean get() = (bits and CAPS_LOCK) != 0
    val scrollLock: Boolean get() = (bits and SCROLL_LOCK) != 0
    val compose: Boolean get() = (bits and COMPOSE) != 0
    val kana: Boolean get() = (bits and KANA) != 0

    private companion object {
        const val NUM_LOCK = 0x01
        const val CAPS_LOCK = 0x02
        const val SCROLL_LOCK = 0x04
        const val COMPOSE = 0x08
        const val KANA = 0x10
    }
}
