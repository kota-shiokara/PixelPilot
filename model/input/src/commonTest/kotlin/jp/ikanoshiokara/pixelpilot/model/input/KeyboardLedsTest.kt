package jp.ikanoshiokara.pixelpilot.model.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeyboardLedsTest {
    @Test
    fun bitsFollowLedUsageOrder() {
        // LED の Usage: Num Lock(1)〜Kana(5)を bit 0〜4 に割り当てる(docs/design/v2.0-standard.md 2.1)
        val leds = KeyboardLeds(0b0_0010)
        assertTrue(leds.capsLock)
        assertFalse(leds.numLock)
        assertFalse(leds.scrollLock)

        assertEquals(
            listOf(true, true, true, true, true),
            KeyboardLeds(0x1F).let { listOf(it.numLock, it.capsLock, it.scrollLock, it.compose, it.kana) },
        )
    }

    @Test
    fun defaultIsAllOff() {
        val leds = KeyboardLeds()
        assertFalse(leds.numLock || leds.capsLock || leds.scrollLock || leds.compose || leds.kana)
    }
}
