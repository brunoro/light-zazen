package net.hitnail.zazen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZazenSettingsTest {
    @Test
    fun defaultsAreTwoAndFortyMinutes() {
        val settings = ZazenSettings()

        assertEquals(2, settings.settleMinutes)
        assertEquals(40, settings.zazenMinutes)
    }

    @Test
    fun customModesUseCustomMinutes() {
        val settings = ZazenSettings(
            settleMode = SettleMode.Custom,
            settleCustomMinutes = 7,
            zazenMode = ZazenMode.Custom,
            zazenCustomMinutes = 55,
        )

        assertEquals(7, settings.settleMinutes)
        assertEquals(55, settings.zazenMinutes)
    }

    @Test
    fun skipSettleModeIsSelectedAndHasNoSettleDuration() {
        val settings = ZazenSettings(settleMode = SettleMode.Skip)

        assertEquals(true, settings.skipSettle)
        assertEquals(0, settings.settleMinutes)
    }

    @Test
    fun legacySkipPreferenceOverridesSavedSettleMode() {
        assertEquals(SettleMode.Skip, settleModeFromPreferences("Two", legacySkip = true))
        assertEquals(SettleMode.Two, settleModeFromPreferences("Two", legacySkip = false))
        assertEquals(SettleMode.Two, settleModeFromPreferences("One", legacySkip = false))
    }

    @Test
    fun parseMinutesAcceptsRange() {
        assertEquals(1, parseMinutes("1"))
        assertEquals(180, parseMinutes(" 180 "))
        assertNull(parseMinutes("0"))
        assertNull(parseMinutes("181"))
        assertNull(parseMinutes("ten"))
        assertNull(parseMinutes(""))
    }
}
