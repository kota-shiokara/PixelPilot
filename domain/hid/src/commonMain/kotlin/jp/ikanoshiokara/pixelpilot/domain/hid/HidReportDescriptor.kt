package jp.ikanoshiokara.pixelpilot.domain.hid

/**
 * キーボード・マウス・Consumer Control の複合 HID デバイスの Report Descriptor。
 * 内容は docs/design/v2.0-standard.md 2. HID デバイス構成のとおり。
 *
 * このバイト列を変えると、ホスト側で再ペアリングが必要になることがある。変更する場合は単体テスト
 * (`HidReportDescriptorTest`)も合わせて更新する。
 */
object HidReportDescriptor {
    /** キーボード(Report ID 1)。Input 8 バイト、Output 1 バイト(LED)。 */
    private val keyboard = bytesOf(
        0x05, 0x01, //       Usage Page (Generic Desktop)
        0x09, 0x06, //       Usage (Keyboard)
        0xA1, 0x01, //       Collection (Application)
        0x85, ReportId.KEYBOARD, // Report ID
        0x05, 0x07, //         Usage Page (Keyboard/Keypad)
        0x19, 0xE0, //         Usage Minimum (Left Control)
        0x29, 0xE7, //         Usage Maximum (Right GUI)
        0x15, 0x00, //         Logical Minimum (0)
        0x25, 0x01, //         Logical Maximum (1)
        0x75, 0x01, //         Report Size (1)
        0x95, 0x08, //         Report Count (8)
        0x81, 0x02, //         Input (Data, Var, Abs): 修飾キー
        0x95, 0x01, //         Report Count (1)
        0x75, 0x08, //         Report Size (8)
        0x81, 0x01, //         Input (Const): 予約
        0x95, 0x05, //         Report Count (5)
        0x75, 0x01, //         Report Size (1)
        0x05, 0x08, //         Usage Page (LEDs)
        0x19, 0x01, //         Usage Minimum (Num Lock)
        0x29, 0x05, //         Usage Maximum (Kana)
        0x91, 0x02, //         Output (Data, Var, Abs): LED
        0x95, 0x01, //         Report Count (1)
        0x75, 0x03, //         Report Size (3)
        0x91, 0x01, //         Output (Const): パディング
        0x95, 0x06, //         Report Count (6)
        0x75, 0x08, //         Report Size (8)
        0x15, 0x00, //         Logical Minimum (0)
        0x26, 0xFF, 0x00, //   Logical Maximum (255)
        0x05, 0x07, //         Usage Page (Keyboard/Keypad)
        0x19, 0x00, //         Usage Minimum (0)
        0x29, 0xFF, //         Usage Maximum (255): JIS キー(International・LANG)を含めるため
        0x81, 0x00, //         Input (Data, Array, Abs): キー x6
        0xC0, //             End Collection
    )

    /** マウス(Report ID 2)。Input 7 バイト。 */
    private val mouse = bytesOf(
        0x05, 0x01, //       Usage Page (Generic Desktop)
        0x09, 0x02, //       Usage (Mouse)
        0xA1, 0x01, //       Collection (Application)
        0x85, ReportId.MOUSE, // Report ID
        0x09, 0x01, //         Usage (Pointer)
        0xA1, 0x00, //         Collection (Physical)
        0x05, 0x09, //           Usage Page (Button)
        0x19, 0x01, //           Usage Minimum (1)
        0x29, 0x05, //           Usage Maximum (5): 左・右・中・戻る・進む
        0x15, 0x00, //           Logical Minimum (0)
        0x25, 0x01, //           Logical Maximum (1)
        0x95, 0x05, //           Report Count (5)
        0x75, 0x01, //           Report Size (1)
        0x81, 0x02, //           Input (Data, Var, Abs): ボタン
        0x95, 0x01, //           Report Count (1)
        0x75, 0x03, //           Report Size (3)
        0x81, 0x01, //           Input (Const): パディング
        0x05, 0x01, //           Usage Page (Generic Desktop)
        0x09, 0x30, //           Usage (X)
        0x09, 0x31, //           Usage (Y)
        0x16, 0x01, 0x80, //     Logical Minimum (-32767)
        0x26, 0xFF, 0x7F, //     Logical Maximum (32767)
        0x75, 0x10, //           Report Size (16)
        0x95, 0x02, //           Report Count (2)
        0x81, 0x06, //           Input (Data, Var, Rel): X, Y
        0x09, 0x38, //           Usage (Wheel)
        0x15, 0x81, //           Logical Minimum (-127)
        0x25, 0x7F, //           Logical Maximum (127)
        0x75, 0x08, //           Report Size (8)
        0x95, 0x01, //           Report Count (1)
        0x81, 0x06, //           Input (Data, Var, Rel): ホイール
        0x05, 0x0C, //           Usage Page (Consumer)
        0x0A, 0x38, 0x02, //     Usage (AC Pan)
        0x15, 0x81, //           Logical Minimum (-127)
        0x25, 0x7F, //           Logical Maximum (127)
        0x75, 0x08, //           Report Size (8)
        0x95, 0x01, //           Report Count (1)
        0x81, 0x06, //           Input (Data, Var, Rel): 横スクロール
        0xC0, //               End Collection
        0xC0, //             End Collection
    )

    /** Consumer Control(Report ID 3)。Input 2 バイト。 */
    private val consumer = bytesOf(
        0x05, 0x0C, //       Usage Page (Consumer)
        0x09, 0x01, //       Usage (Consumer Control)
        0xA1, 0x01, //       Collection (Application)
        0x85, ReportId.CONSUMER, // Report ID
        0x15, 0x00, //         Logical Minimum (0)
        0x26, 0xFF, 0x03, //   Logical Maximum (0x3FF)
        0x19, 0x00, //         Usage Minimum (0)
        0x2A, 0xFF, 0x03, //   Usage Maximum (0x3FF)
        0x75, 0x10, //         Report Size (16)
        0x95, 0x01, //         Report Count (1)
        0x81, 0x00, //         Input (Data, Array, Abs)
        0xC0, //             End Collection
    )

    private val descriptor: ByteArray = keyboard + mouse + consumer

    /** Report Descriptor のバイト列。`BluetoothHidDeviceAppSdpSettings` に渡す。呼ぶたびに複製を返す。 */
    val bytes: ByteArray
        get() = descriptor.copyOf()

    private fun bytesOf(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }
}

/** Report ID。`BluetoothHidDevice.sendReport` の `id` に渡す(`data` には含めない)。 */
object ReportId {
    const val KEYBOARD = 1
    const val MOUSE = 2
    const val CONSUMER = 3
}

/** 各レポートの長さ(バイト数、Report ID を除く)。 */
object ReportLength {
    const val KEYBOARD_INPUT = 8
    const val KEYBOARD_OUTPUT = 1
    const val MOUSE_INPUT = 7
    const val CONSUMER_INPUT = 2
}
