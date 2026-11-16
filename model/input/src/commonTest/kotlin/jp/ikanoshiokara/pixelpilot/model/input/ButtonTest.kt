package jp.ikanoshiokara.pixelpilot.model.input

import kotlin.test.Test
import kotlin.test.assertEquals

class ButtonTest {
    @Test
    fun usagesMatchHidButtonPage() {
        // マウスのレポートのボタン欄は Usage 1〜5 を bit 0〜4 に割り当てる(docs/design/v2.0-standard.md 2.2)
        assertEquals(
            listOf(1, 2, 3, 4, 5),
            listOf(Button.LEFT, Button.RIGHT, Button.MIDDLE, Button.BACK, Button.FORWARD).map { it.usage },
        )
    }
}
