package net.hitnail.zazen

import androidx.compose.foundation.background
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens

/** Returns minutes in [MIN_MINUTES]..[MAX_MINUTES]; invalid input keeps the editor open. */
class MinutesInputScreen(
    sealedActivity: SealedLightActivity,
    private val title: String,
    private val initialMinutes: Int,
) : SimpleLightScreen<Int>(sealedActivity) {

    @Composable
    override fun Content() {
        val colors by LightThemeController.colors.collectAsState()
        val keyboardOptions = rememberKeyboardOptions()
        val textState = rememberTextFieldState(initialMinutes.toString())
        var invalid by remember { mutableStateOf(false) }

        LightTheme(colors = colors) {
            LightTextInputEditor(
                title = if (invalid) "$MIN_MINUTES–$MAX_MINUTES minutes" else title,
                state = textState,
                keyboardOptionsFlow = keyboardOptions,
                onSubmit = { text ->
                    val minutes = parseMinutes(text)
                    if (minutes == null) invalid = true else goBack(minutes)
                },
                onBack = { goBack() },
                submitLabel = "DONE",
                singleLine = true,
                modifier = Modifier.background(LightThemeTokens.colors.background),
            )
        }
    }
}
