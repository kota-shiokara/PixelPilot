package jp.ikanoshiokara.pixelpilot.model.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeyboardUsageTest {
    @Test
    fun lettersAndDigitsAreContiguous() {
        assertEquals(25, KeyboardUsage.Z - KeyboardUsage.A)
        assertEquals(8, KeyboardUsage.DIGIT_9 - KeyboardUsage.DIGIT_1)
        assertEquals(KeyboardUsage.DIGIT_9 + 1, KeyboardUsage.DIGIT_0)
        assertEquals(11, KeyboardUsage.F12 - KeyboardUsage.F1)
    }

    @Test
    fun onlyLeftControlToRightGuiAreModifiers() {
        (0xE0..0xE7).forEach { assertTrue(KeyboardUsage.isModifier(it), "0x${it.toString(16)}") }
        assertFalse(KeyboardUsage.isModifier(0xDF))
        assertFalse(KeyboardUsage.isModifier(0xE8))
        assertFalse(KeyboardUsage.isModifier(KeyboardUsage.CAPS_LOCK))
    }

    @Test
    fun modifierBitFollowsHidOrder() {
        assertEquals(0x01, KeyboardUsage.modifierBit(KeyboardUsage.LEFT_CONTROL))
        assertEquals(0x02, KeyboardUsage.modifierBit(KeyboardUsage.LEFT_SHIFT))
        assertEquals(0x04, KeyboardUsage.modifierBit(KeyboardUsage.LEFT_ALT))
        assertEquals(0x08, KeyboardUsage.modifierBit(KeyboardUsage.LEFT_GUI))
        assertEquals(0x10, KeyboardUsage.modifierBit(KeyboardUsage.RIGHT_CONTROL))
        assertEquals(0x20, KeyboardUsage.modifierBit(KeyboardUsage.RIGHT_SHIFT))
        assertEquals(0x40, KeyboardUsage.modifierBit(KeyboardUsage.RIGHT_ALT))
        assertEquals(0x80, KeyboardUsage.modifierBit(KeyboardUsage.RIGHT_GUI))
    }

    @Test
    fun modifierBitRejectsNonModifier() {
        assertFailsWith<IllegalArgumentException> {
            KeyboardUsage.modifierBit(KeyboardUsage.A)
        }
    }
}
