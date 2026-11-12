package net.hitnail.zazen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
                    OptionRow {
                        OptionButton("skip", settings.settleMode == SettleMode.Skip, Modifier.weight(1f)) {
                            viewModel.update { it.copy(settleMode = SettleMode.Skip) }
                        }
                        OptionButton("2 min", settings.settleMode == SettleMode.Two, Modifier.weight(1f)) {
                            viewModel.update { it.copy(settleMode = SettleMode.Two) }
                        }
                        CustomTimingButton(
                            minutes = settings.settleCustomMinutes,
                            selected = settings.settleMode == SettleMode.Custom,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (settings.settleMode != SettleMode.Custom) {
                                viewModel.update { it.copy(settleMode = SettleMode.Custom) }
                            }
                            editMinutes("Settle minutes", settings.settleCustomMinutes) { minutes ->
                                viewModel.update { it.copy(settleCustomMinutes = minutes) }
                            }
                        }
                    }

                    SectionLabel("ZAZEN")
                    OptionRow {
                        OptionButton("20 min", settings.zazenMode == ZazenMode.Twenty, Modifier.weight(1f)) {
                            viewModel.update { it.copy(zazenMode = ZazenMode.Twenty) }
                        }
                        OptionButton("40 min", settings.zazenMode == ZazenMode.Forty, Modifier.weight(1f)) {
                            viewModel.update { it.copy(zazenMode = ZazenMode.Forty) }
                        }
                        CustomTimingButton(
                            minutes = settings.zazenCustomMinutes,
                            selected = settings.zazenMode == ZazenMode.Custom,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (settings.zazenMode != ZazenMode.Custom) {
                                viewModel.update { it.copy(zazenMode = ZazenMode.Custom) }
                            }
                            editMinutes("Zazen minutes", settings.zazenCustomMinutes) { minutes ->
                                viewModel.update { it.copy(zazenCustomMinutes = minutes) }
                            }
                        }
                    }

                    SectionLabel("ALERT")
                    OptionRow {
                        AlertMode.entries.forEach { mode ->
                            OptionButton(
                                label = mode.name.lowercase(),
                                selected = settings.alert == mode,
                                modifier = Modifier.weight(1f),
                            ) {
                                viewModel.update { it.copy(alert = mode) }
                            }
                        }
                    }
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

@Composable
private fun SectionLabel(text: String) {
    LightText(
        text = text,
        variant = LightTextVariant.Detail,
        modifier = Modifier.padding(top = 1.5f.gridUnitsAsDp(), bottom = 0.5f.gridUnitsAsDp()),
    )
}

@Composable
private fun OptionRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), content = content)
}

@Composable
private fun OptionButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    LightText(
        text = label,
        variant = LightTextVariant.Copy,
        underline = selected,
        lighten = !selected,
        align = TextAlign.Center,
        modifier = modifier
            .lightClickable(onClick = onClick)
            .padding(vertical = 0.5f.gridUnitsAsDp()),
    )
}

@Composable
private fun CustomTimingButton(
    minutes: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .lightClickable(onClick = onClick)
            .padding(vertical = 0.5f.gridUnitsAsDp()),
        contentAlignment = Alignment.Center,
    ) {
        LightText(
            text = if (selected) "$minutes min" else "custom",
            variant = LightTextVariant.Copy,
            underline = selected,
            lighten = !selected,
            align = TextAlign.Center,
        )
        if (selected) {
            LightText(
                text = "custom:",
                variant = LightTextVariant.Detail,
                align = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-1.1f).gridUnitsAsDp()),
            )
        }
    }
}
