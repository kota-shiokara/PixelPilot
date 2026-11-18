package jp.ikanoshiokara.pixelpilot.model.input

/**
 * Consumer Page(0x0C)の Usage。値は USB HID Usage Tables に従う。
 * Consumer Control のレポートは、押下時にこの値、解放時に [NONE] を送る。
 */
object ConsumerUsage {
    /** 何も押していない(解放)。 */
    const val NONE = 0x0000

    /** 画面の明るさ +(ホストによっては効かない) */
    const val BRIGHTNESS_UP = 0x006F

    /** 画面の明るさ −(ホストによっては効かない) */
    const val BRIGHTNESS_DOWN = 0x0070

    const val SCAN_NEXT_TRACK = 0x00B5
    const val SCAN_PREVIOUS_TRACK = 0x00B6
    const val STOP = 0x00B7
    const val PLAY_PAUSE = 0x00CD
    const val MUTE = 0x00E2
    const val VOLUME_UP = 0x00E9
    const val VOLUME_DOWN = 0x00EA

    /** 横スクロール(AC Pan)。マウスのレポートの横スクロール欄の Usage で、キーとしては送らない。 */
    const val AC_PAN = 0x0238
}
