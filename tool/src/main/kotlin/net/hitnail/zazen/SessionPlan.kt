package net.hitnail.zazen

/** Time between consecutive bell strikes. */
const val BELL_SPACING_MS = 5_000L
const val MAX_OPENING_STRIKE_JITTER_MS = 1_000L
const val OPENING_BELLS = 3
private const val MINUTE_MS = 60_000L

enum class Phase { Settle, OpeningBells, Zazen, ClosingBell }

enum class BellStrike(val vibrationScale: Float) {
    OpeningFirst(0.75f),
    OpeningSecond(1f),
    OpeningThird(0.5f),
    Closing(1f),
}

sealed interface Clip {
    val durationMs: Long

    data class Bell(override val durationMs: Long) : Clip
    data class Silence(override val durationMs: Long) : Clip
}

/** One audio queue item. [strike] marks where a bell sounds and/or vibrates. */
data class Segment(val phase: Phase, val clip: Clip, val strike: Boolean = false)

data class Progress(
    val phase: Phase,
    /** Time left in [phase]. */
    val remainingMs: Long,
    /** 1-based number of the latest strike in [phase], or 0 before the first. */
    val bell: Int,
)

/**
 * A whole session as one audio queue, so playback keeps time while the phone
 * is locked. Settle segments are always present; skipping settle starts the
 * queue at [openingBellsIndex].
 */
class SessionPlan(
    val segments: List<Segment>,
    val startIndex: Int,
    val zazenMinutes: Int,
) {
    val openingBellsIndex: Int = segments.indexOfFirst { it.phase == Phase.OpeningBells }

    fun bellStrikeAt(index: Int): BellStrike? {
        val segment = segments[index]
        if (!segment.strike) return null
        return when (segment.phase) {
            Phase.OpeningBells -> when (
                segments.take(index + 1).count { it.phase == Phase.OpeningBells && it.strike }
            ) {
                1 -> BellStrike.OpeningFirst
                2 -> BellStrike.OpeningSecond
                3 -> BellStrike.OpeningThird
                else -> null
            }
            Phase.ClosingBell -> BellStrike.Closing
            else -> null
        }
    }

    fun progressAt(index: Int, positionMs: Long): Progress {
        val segment = segments[index]
        val currentMs = (segment.clip.durationMs - positionMs).coerceIn(0L, segment.clip.durationMs)
        val laterMs = segments.subList(index + 1, segments.size)
            .takeWhile { it.phase == segment.phase }
            .sumOf { it.clip.durationMs }
        val bell = segments.subList(0, index + 1).count { it.phase == segment.phase && it.strike }
        return Progress(segment.phase, currentMs + laterMs, bell)
    }
}

/**
 * A bell longer than [BELL_SPACING_MS] gets no trailing silence, which
 * stretches the spacing to the bell's own length.
 */
fun buildSessionPlan(
    settleMinutes: Int,
    zazenMinutes: Int,
    skipSettle: Boolean,
    alert: AlertMode,
    bellDurationMs: Long,
    openingStrikeSpacingsMs: List<Long> = List(OPENING_BELLS - 1) { BELL_SPACING_MS },
): SessionPlan {
    require(bellDurationMs > 0L) { "Bell must have a duration" }
    require(openingStrikeSpacingsMs.size == OPENING_BELLS - 1) {
        "Expected ${OPENING_BELLS - 1} opening strike spacings"
    }
    require(openingStrikeSpacingsMs.all { it > 0L }) { "Opening strike spacings must be positive" }

    fun bellSlot(phase: Phase, slotDurationMs: Long, trailingSilence: Boolean): List<Segment> = if (alert.sound) {
        listOfNotNull(
            Segment(phase, Clip.Bell(bellDurationMs), strike = true),
            (slotDurationMs - bellDurationMs)
                .takeIf { trailingSilence && it > 0L }
                ?.let { Segment(phase, Clip.Silence(it)) },
        )
    } else {
        listOf(Segment(phase, Clip.Silence(slotDurationMs), strike = true))
    }

    fun minutes(phase: Phase, count: Int) = List(count) { Segment(phase, Clip.Silence(MINUTE_MS)) }

    val segments = buildList {
        addAll(minutes(Phase.Settle, settleMinutes))
        repeat(OPENING_BELLS) { strike ->
            val spacing = openingStrikeSpacingsMs.getOrElse(strike) { BELL_SPACING_MS }
            addAll(bellSlot(Phase.OpeningBells, spacing, trailingSilence = true))
        }
        addAll(minutes(Phase.Zazen, zazenMinutes))
        addAll(bellSlot(Phase.ClosingBell, BELL_SPACING_MS, trailingSilence = false))
    }
    val plan = SessionPlan(segments, startIndex = 0, zazenMinutes = zazenMinutes)
    return if (skipSettle) SessionPlan(segments, plan.openingBellsIndex, zazenMinutes) else plan
}

/** `m:ss`, rounding up so `0:00` only shows once time is actually up. */
fun formatClock(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0L) + 999L) / 1_000L
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}
