package net.hitnail.zazen

import kotlin.test.Test
import kotlin.test.assertEquals

class SessionPlanTest {
    private fun plan(
        settle: Int = 2,
        zazen: Int = 40,
        skip: Boolean = false,
        alert: AlertMode = AlertMode.Bell,
        bellMs: Long = 2_500L,
    ) = buildSessionPlan(settle, zazen, skip, alert, bellMs)

    @Test
    fun bellModeQueuesBellsPaddedToFiveSeconds() {
        val segments = plan(settle = 1, zazen = 1).segments

        assertEquals(
            listOf(
                Segment(Phase.Settle, Clip.Silence(60_000L)),
                Segment(Phase.OpeningBells, Clip.Bell(2_500L), strike = true),
                Segment(Phase.OpeningBells, Clip.Silence(2_500L)),
                Segment(Phase.OpeningBells, Clip.Bell(2_500L), strike = true),
                Segment(Phase.OpeningBells, Clip.Silence(2_500L)),
                Segment(Phase.OpeningBells, Clip.Bell(2_500L), strike = true),
                Segment(Phase.OpeningBells, Clip.Silence(2_500L)),
                Segment(Phase.Zazen, Clip.Silence(60_000L)),
                Segment(Phase.ClosingBell, Clip.Bell(2_500L), strike = true),
            ),
            segments,
        )
    }

    @Test
    fun strikesAreFiveSecondsApart() {
        val segments = plan(bellMs = 3_100L).segments
        val strikeStarts = segments.runningFold(0L) { start, segment -> start + segment.clip.durationMs }
            .zip(segments)
            .filter { (_, segment) -> segment.phase == Phase.OpeningBells && segment.strike }
            .map { (start, _) -> start }

        assertEquals(listOf(BELL_SPACING_MS, BELL_SPACING_MS), strikeStarts.zipWithNext { a, b -> b - a })
    }

    @Test
    fun fiveSecondBellNeedsNoPadding() {
        val opening = plan(bellMs = BELL_SPACING_MS).segments.filter { it.phase == Phase.OpeningBells }

        assertEquals(List(OPENING_BELLS) { Segment(Phase.OpeningBells, Clip.Bell(5_000L), strike = true) }, opening)
    }

    @Test
    fun vibrateModeUsesSilentStrikeSlots() {
        val segments = plan(settle = 1, zazen = 1, alert = AlertMode.Vibrate).segments

        assertEquals(
            listOf(
                Segment(Phase.Settle, Clip.Silence(60_000L)),
                Segment(Phase.OpeningBells, Clip.Silence(5_000L), strike = true),
                Segment(Phase.OpeningBells, Clip.Silence(5_000L), strike = true),
                Segment(Phase.OpeningBells, Clip.Silence(5_000L), strike = true),
                Segment(Phase.Zazen, Clip.Silence(60_000L)),
                Segment(Phase.ClosingBell, Clip.Silence(5_000L), strike = true),
            ),
            segments,
        )
    }

    @Test
    fun skippingSettleStartsAtFirstBell() {
        val plan = plan(settle = 2, skip = true)

        assertEquals(2, plan.startIndex)
        assertEquals(plan.openingBellsIndex, plan.startIndex)
        assertEquals(0, plan(skip = false).startIndex)
    }

    @Test
    fun progressCountsDownWithinPhase() {
        val plan = plan(settle = 2, zazen = 40)

        assertEquals(Progress(Phase.Settle, 120_000L, 0), plan.progressAt(0, 0L))
        assertEquals(Progress(Phase.Settle, 18_000L, 0), plan.progressAt(1, 42_000L))
        val zazenStart = plan.segments.indexOfFirst { it.phase == Phase.Zazen }
        assertEquals(Progress(Phase.Zazen, 2_400_000L, 0), plan.progressAt(zazenStart, 0L))
        assertEquals(Progress(Phase.Zazen, 59_000L, 0), plan.progressAt(zazenStart + 39, 1_000L))
    }

    @Test
    fun progressNumbersBells() {
        val plan = plan()
        val first = plan.openingBellsIndex

        assertEquals(1, plan.progressAt(first, 0L).bell)
        assertEquals(1, plan.progressAt(first + 1, 0L).bell)
        assertEquals(2, plan.progressAt(first + 2, 0L).bell)
        assertEquals(3, plan.progressAt(first + 4, 0L).bell)
        assertEquals(1, plan.progressAt(plan.segments.lastIndex, 0L).bell)
    }

    @Test
    fun formatClockRoundsUp() {
        assertEquals("40:00", formatClock(2_400_000L))
        assertEquals("1:42", formatClock(101_001L))
        assertEquals("0:01", formatClock(1L))
        assertEquals("0:00", formatClock(0L))
    }
}
