package net.hitnail.zazen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val store: ZazenSettingsStore) : LightViewModel<Unit>() {
    val settings: StateFlow<ZazenSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, ZazenSettings())
    val session: StateFlow<SessionState> = ZazenSession.state

    fun update(transform: (ZazenSettings) -> ZazenSettings) {
        viewModelScope.launch { store.update(transform) }
    }
}

@InitialScreen
class HomeScreen(private val sealedActivity: SealedLightActivity) :
    LightScreen<Unit, HomeViewModel>(sealedActivity) {

    override val viewModelClass = HomeViewModel::class.java
    override fun createViewModel() = HomeViewModel(ZazenSettingsStore(lightContext.dataStore))

    private fun startOrReturn() {
        if (ZazenSession.state.value == SessionState.Idle) {
            ZazenSession.start(sealedActivity, lightContext, viewModel.settings.value)
        }
        navigateTo(::SessionScreen)
    }

    private fun editMinutes(title: String, initial: Int, onResult: (Int) -> Unit) {
        navigateTo({ MinutesInputScreen(it, title, initial) }, onResult)
    }

    @Composable
    override fun Content() {
        val colors by LightThemeController.colors.collectAsState()
        val settings by viewModel.settings.collectAsState()
        val session by viewModel.session.collectAsState()

        LightTheme(colors = colors) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(center = LightTopBarCenter.Text("Zazen"))
                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                ) {
                    SectionLabel("SETTLE")
                    OptionRow(
                        SettleMode.entries.map { mode ->
                            Option(
                                label = minutesLabel(mode.minutes, settings.settleCustomMinutes, settings.settleMode == mode),
                                selected = settings.settleMode == mode,
                                enabled = !settings.skipSettle,
                            ) {
                                if (mode == SettleMode.Custom && settings.settleMode == mode) {
                                    editMinutes("Settle minutes", settings.settleCustomMinutes) { minutes ->
                                        viewModel.update { it.copy(settleCustomMinutes = minutes) }
                                    }
                                } else {
                                    viewModel.update { it.copy(settleMode = mode) }
                                }
                            }
                        },
                    )
                    Toggle("skip", settings.skipSettle) {
                        viewModel.update { it.copy(skipSettle = !it.skipSettle) }
                    }

                    SectionLabel("ZAZEN")
                    OptionRow(
                        ZazenMode.entries.map { mode ->
                            Option(
                                label = minutesLabel(mode.minutes, settings.zazenCustomMinutes, settings.zazenMode == mode),
                                selected = settings.zazenMode == mode,
                            ) {
                                if (mode == ZazenMode.Custom && settings.zazenMode == mode) {
                                    editMinutes("Zazen minutes", settings.zazenCustomMinutes) { minutes ->
                                        viewModel.update { it.copy(zazenCustomMinutes = minutes) }
                                    }
                                } else {
                                    viewModel.update { it.copy(zazenMode = mode) }
                                }
                            }
                        },
                    )

                    SectionLabel("ALERT")
                    OptionRow(
                        AlertMode.entries.map { mode ->
                            Option(label = mode.name.lowercase(), selected = settings.alert == mode) {
                                viewModel.update { it.copy(alert = mode) }
                            }
                        },
                    )
                }
                LightText(
                    text = if (session == SessionState.Idle) "START" else "RETURN",
                    variant = LightTextVariant.Button,
                    align = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .lightClickable(onClick = ::startOrReturn)
                        .padding(vertical = 1.5f.gridUnitsAsDp()),
                )
            }
        }
    }
}

/** A selected custom option shows its length; otherwise it reads "custom". */
private fun minutesLabel(presetMinutes: Int?, customMinutes: Int, selected: Boolean): String = when {
    presetMinutes != null -> "$presetMinutes min"
    selected -> "$customMinutes min"
    else -> "custom"
}

private class Option(
    val label: String,
    val selected: Boolean,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
private fun SectionLabel(text: String) {
    LightText(
        text = text,
        variant = LightTextVariant.Detail,
        modifier = Modifier.padding(top = 1.5f.gridUnitsAsDp(), bottom = 0.5f.gridUnitsAsDp()),
    )
}

@Composable
private fun OptionRow(options: List<Option>) {
    Row(Modifier.fillMaxWidth()) {
        options.forEach { option ->
            LightText(
                text = option.label,
                variant = LightTextVariant.Copy,
                underline = option.selected,
                lighten = !option.selected || !option.enabled,
                modifier = Modifier
                    .weight(1f)
                    .lightClickable(enabled = option.enabled, onClick = option.onClick)
                    .padding(vertical = 0.5f.gridUnitsAsDp()),
            )
        }
    }
}

@Composable
private fun Toggle(label: String, on: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .lightClickable(onClick = onClick)
            .padding(vertical = 0.5f.gridUnitsAsDp()),
    ) {
        LightIcon(
            icon = if (on) LightIcons.TOGGLE_STATE_ON else LightIcons.TOGGLE_STATE_OFF,
            modifier = Modifier.padding(end = 0.5f.gridUnitsAsDp()),
        )
        LightText(text = label, variant = LightTextVariant.Copy)
    }
}
