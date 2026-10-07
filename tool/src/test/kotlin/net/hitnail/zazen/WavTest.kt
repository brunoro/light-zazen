package net.hitnail.zazen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WavTest {
    @Test
    fun silenceRoundTripsDuration() {
        listOf(1L, 2_500L, 5_000L, 60_000L).forEach { durationMs ->
            assertEquals(durationMs, wavDurationMs(silenceWav(durationMs)))
        }
    }

    @Test
    fun silenceIsAllZeroSamples() {
        val wav = silenceWav(100L)

        assertEquals(44 + 1_600, wav.size)
        assertTrue(wav.drop(44).all { it == 0.toByte() })
    }

    @Test
    fun bundledBellFitsInStrikeSpacing() {
        val durationMs = wavDurationMs(File("src/main/assets/audio/bell.wav").readBytes())

        assertTrue(durationMs in 1L..BELL_SPACING_MS, "bell.wav is ${durationMs}ms")
    }

    @Test
    fun rejectsNonWav() {
        assertFailsWith<IllegalArgumentException> { wavDurationMs(ByteArray(64)) }
    }
}
