package net.hitnail.zazen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

/** Going back leaves the session running; HOME on the done view ends it. */
class SessionScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    override fun onScreenDestroy() {
        ZazenSession.dismiss()
    }

    @Composable
    override fun Content() {
        val colors by LightThemeController.colors.collectAsState()
        val state by ZazenSession.state.collectAsState()

        LaunchedEffect(state) {
            if (state == SessionState.Idle) goBack()
        }

        LightTheme(colors = colors) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .padding(horizontal = 1f.gridUnitsAsDp()),
            ) {
                when (val current = state) {
                    is SessionState.Running -> Running(current)
                    is SessionState.Done -> Done(current)
                    SessionState.Idle -> Unit
                }
            }
        }
    }

    @Composable
    private fun Running(state: SessionState.Running) {
        val progress = state.progress
        val zazenClock = formatClock(state.zazenMinutes * 60_000L)
        val (label, clock) = when (progress.phase) {
            Phase.Settle -> "settle" to formatClock(progress.remainingMs)
            Phase.OpeningBells -> "bell ${progress.bell}/$OPENING_BELLS" to zazenClock
            Phase.Zazen -> "zazen" to formatClock(progress.remainingMs)
            Phase.ClosingBell -> "bell" to formatClock(0L)
        }
        CenteredText(label, LightTextVariant.Copy)
        CenteredText(clock, LightTextVariant.Title, monospace = true)
        if (progress.phase == Phase.Settle) {
            CenteredText("then $zazenClock", LightTextVariant.Detail, lighten = true)
        }
        Spacer(Modifier.padding(top = 3f.gridUnitsAsDp()))
        if (progress.phase == Phase.Settle) {
            Button("SKIP", ZazenSession::skipSettle)
        }
        Button("STOP", ZazenSession::stop)
    }

    @Composable
    private fun Done(state: SessionState.Done) {
        CenteredText("done", LightTextVariant.Copy)
        CenteredText("${state.zazenMinutes} min", LightTextVariant.Title)
        Spacer(Modifier.padding(top = 3f.gridUnitsAsDp()))
        Button("HOME") { goBack() }
    }
}

@Composable
private fun CenteredText(
    text: String,
    variant: LightTextVariant,
    lighten: Boolean = false,
    monospace: Boolean = false,
) {
    LightText(
        text = text,
        variant = variant,
        align = TextAlign.Center,
        lighten = lighten,
        monospace = monospace,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.5f.gridUnitsAsDp()),
    )
}

@Composable
private fun Button(text: String, onClick: () -> Unit) {
    LightText(
        text = text,
        variant = LightTextVariant.Button,
        align = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 1f.gridUnitsAsDp()),
    )
}
