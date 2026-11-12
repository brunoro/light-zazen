package net.hitnail.zazen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BellEnvelopeTest {
    @Test
    fun waveformSpansFiveSeconds() {
        assertEquals(BELL_SPACING_MS, bellWaveform.duration.inWholeMilliseconds)
    }

    @Test
    fun waveformStrikesHardThenNeverRises() {
        val amplitudes = bellWaveform.amplitudes.map { it.value }

        assertEquals(255, amplitudes.first())
        assertTrue(amplitudes.zipWithNext().all { (a, b) -> b <= a })
        assertTrue(amplitudes.last() < 10)
    }

    @Test
    fun amplitudeInterpolatesBetweenKeyframes() {
        assertEquals(255, bellAmplitudeAt(50L))
        assertEquals(120, bellAmplitudeAt(1_150L))
        assertEquals(0, bellAmplitudeAt(6_000L))
    }
}
