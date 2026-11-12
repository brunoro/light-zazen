package net.hitnail.zazen

import android.util.Log
import com.thelightphone.sdk.LightVibrationUsage
import com.thelightphone.sdk.LightVibrator
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SealedLightContext
import com.thelightphone.sdk.audio.DefaultLightAudio
import com.thelightphone.sdk.audio.LightAudioItem
import com.thelightphone.sdk.audio.LightAudioPlayback
import com.thelightphone.sdk.audio.LightAudioPlayer
import com.thelightphone.sdk.audio.LightAudioSource
import com.thelightphone.sdk.audio.LightAudioUsage
import com.thelightphone.sdk.audio.LightMediaMetadata
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ZazenSession"
private const val BELL_ASSET = "audio/bell.wav"
private const val MEDIUM_BELL_ASSET = "audio/bell-medium.wav"
private const val SOFT_BELL_ASSET = "audio/bell-soft.wav"
private const val END_TOLERANCE_MS = 300L

sealed interface SessionState {
    data object Idle : SessionState
    data class Running(val progress: Progress, val zazenMinutes: Int) : SessionState
    data class Done(val zazenMinutes: Int) : SessionState
}

/**
 * Process-wide, so the session and its vibrations outlive the session screen.
 * The detached player's foreground service keeps the process alive and the
 * CPU awake while the queue plays, including when the phone is locked.
 */
object ZazenSession {
    private val scope = MainScope()
    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var current: Session? = null

    private class Session(
        val plan: SessionPlan,
        val player: LightAudioPlayer,
        val vibrator: LightVibrator?,
    ) {
        var items: List<LightAudioItem> = emptyList()
        var job: Job? = null
    }

    fun start(sealedActivity: SealedLightActivity, context: SealedLightContext, settings: ZazenSettings) {
        if (_state.value != SessionState.Idle) return
        val plan = buildSessionPlan(
            settleMinutes = settings.settleMinutes,
            zazenMinutes = settings.zazenMinutes,
            skipSettle = settings.skipSettle,
            alert = settings.alert,
            bellDurationMs = wavDurationMs(context.readAsset(BELL_ASSET)),
            openingStrikeSpacingsMs = List(OPENING_BELLS - 1) {
                BELL_SPACING_MS + Random.nextLong(-MAX_OPENING_STRIKE_JITTER_MS, MAX_OPENING_STRIKE_JITTER_MS + 1L)
            },
        )
        val session = Session(
            plan = plan,
            player = DefaultLightAudio(sealedActivity).newPlayer(LightAudioUsage.Alarm, LightAudioPlayback.Detached),
            vibrator = if (settings.alert.vibration) sealedActivity.vibrator else null,
        )
        current = session
        _state.value = SessionState.Running(plan.progressAt(plan.startIndex, 0L), plan.zazenMinutes)
        session.job = scope.launch { run(session, context.filesDir) }
    }

    fun skipSettle() {
        val session = current ?: return
        val running = _state.value as? SessionState.Running ?: return
        if (running.progress.phase != Phase.Settle || session.items.isEmpty()) return
        session.player.setMediaQueue(session.items, session.plan.openingBellsIndex)
        session.player.play()
    }

    fun stop() = end(SessionState.Idle)

    /** Leaves the done state once the user has seen it. */
    fun dismiss() {
        if (_state.value is SessionState.Done) _state.value = SessionState.Idle
    }

    private suspend fun run(session: Session, filesDir: File) {
        val plan = session.plan
        val player = session.player
        session.items = withContext(Dispatchers.IO) {
            plan.segments.mapIndexed { index, segment ->
                segment.toAudioItem(filesDir, plan.bellStrikeAt(index))
            }
        }
        if (!player.awaitReady()) {
            Log.e(TAG, "Player released before it was ready")
            end(SessionState.Idle)
            return
        }
        player.setMediaQueue(session.items, plan.startIndex)
        player.play()

        coroutineScope {
            launch {
                player.currentMediaItemIndex
                    .filter { it in plan.segments.indices }
                    .distinctUntilChanged()
                    .collect { index ->
                        plan.bellStrikeAt(index)?.let { strike ->
                            session.vibrator?.vibrate(bellWaveform(strike), LightVibrationUsage.Alarm)
                        }
                    }
            }
            launch {
                val error = player.error.filterNotNull().first()
                Log.e(TAG, "Playback failed: ${error.kind} ${error.diagnostic}")
                end(SessionState.Idle)
            }
            // Position is polled, so right after an item transition it still
            // holds the previous item's position until the next poll.
            var shownIndex = -1
            var stalePositionMs = -1L
            combine(
                player.currentMediaItemIndex,
                player.positionMs,
                player.durationMs,
                player.isPlaying,
            ) { index, positionMs, durationMs, isPlaying ->
                if (index !in plan.segments.indices) return@combine
                val ended = index == plan.segments.lastIndex && !isPlaying &&
                    durationMs > 0L && positionMs >= durationMs - END_TOLERANCE_MS
                if (ended) {
                    end(SessionState.Done(plan.zazenMinutes))
                    return@combine
                }
                if (index != shownIndex) {
                    shownIndex = index
                    stalePositionMs = positionMs
                }
                val effectivePositionMs = if (positionMs == stalePositionMs) 0L else positionMs
                _state.value = SessionState.Running(plan.progressAt(index, effectivePositionMs), plan.zazenMinutes)
            }.collect {}
        }
    }

    private fun end(next: SessionState) {
        val session = current ?: return
        current = null
        _state.value = next
        session.player.stop()
        session.player.setMediaQueue(emptyList())
        session.player.release()
        // A finished session lets the closing bell's vibration ring out.
        if (next == SessionState.Idle) session.vibrator?.cancel()
        session.job?.cancel()
    }
}

private fun Segment.toAudioItem(filesDir: File, strike: BellStrike?): LightAudioItem {
    val metadata = LightMediaMetadata(
        title = when (phase) {
            Phase.Settle -> "Settle"
            Phase.OpeningBells, Phase.ClosingBell -> "Bell"
            Phase.Zazen -> "Zazen"
        },
        artist = "Zazen",
        durationMs = clip.durationMs,
    )
    val source = when (clip) {
        is Clip.Bell -> LightAudioSource.AssetSource(
            when (strike) {
                BellStrike.OpeningFirst -> MEDIUM_BELL_ASSET
                BellStrike.OpeningThird -> SOFT_BELL_ASSET
                else -> BELL_ASSET
            },
        )
        is Clip.Silence -> LightAudioSource.FileSource(silenceFile(filesDir, clip.durationMs))
    }
    return LightAudioItem(source, metadata)
}

private fun silenceFile(filesDir: File, durationMs: Long): File {
    val file = File(filesDir, "silence/silence-$durationMs.wav")
    if (!file.exists()) {
        file.parentFile?.mkdirs()
        val partial = File(file.parentFile, "${file.name}.partial")
        partial.writeBytes(silenceWav(durationMs))
        partial.renameTo(file)
    }
    return file
}
