package net.hitnail.zazen

import com.thelightphone.sdk.LightVibrationWaveform
import com.thelightphone.sdk.VibrationAmplitude
import kotlin.time.Duration.Companion.milliseconds

private const val STEP_MS = 50L

/**
 * Hand-tuned (timeMs, amplitude) keyframes of a struck bell: a hard strike,
 * a quick drop as the attack fades, then a long tail.
 */
internal val BELL_KEYFRAMES: List<Pair<Long, Int>> = listOf(
    0L to 255,
    80L to 255,
    300L to 190,
    800L to 140,
    1_500L to 100,
    2_500L to 65,
    3_500L to 40,
    4_300L to 22,
    5_000L to 0,
)

/** [BELL_KEYFRAMES] linearly interpolated into [STEP_MS] segments. */
val bellWaveform: LightVibrationWaveform by lazy { bellWaveform(BellStrike.OpeningSecond) }

private val bellWaveforms: Map<BellStrike, LightVibrationWaveform> by lazy {
    BellStrike.entries.associateWith { strike ->
        createBellWaveform(strike.vibrationScale)
    }
}

internal fun bellWaveform(strike: BellStrike): LightVibrationWaveform = bellWaveforms.getValue(strike)

private fun createBellWaveform(amplitudeScale: Float): LightVibrationWaveform {
    val endMs = BELL_KEYFRAMES.last().first
    val steps = (endMs / STEP_MS).toInt()
    return LightVibrationWaveform(
        durations = List(steps) { STEP_MS.milliseconds },
        amplitudes = List(steps) { step ->
            VibrationAmplitude((bellAmplitudeAt(step * STEP_MS) * amplitudeScale).toInt())
        },
    )
}

internal fun bellAmplitudeAt(timeMs: Long): Int {
    val next = BELL_KEYFRAMES.indexOfFirst { it.first > timeMs }
    if (next == -1) return BELL_KEYFRAMES.last().second
    if (next == 0) return BELL_KEYFRAMES.first().second
    val (startMs, startAmplitude) = BELL_KEYFRAMES[next - 1]
    val (endMs, endAmplitude) = BELL_KEYFRAMES[next]
    val fraction = (timeMs - startMs).toFloat() / (endMs - startMs)
    return (startAmplitude + (endAmplitude - startAmplitude) * fraction).toInt()
}
