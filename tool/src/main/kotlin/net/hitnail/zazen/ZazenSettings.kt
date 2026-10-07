package net.hitnail.zazen

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val MIN_MINUTES = 1
const val MAX_MINUTES = 180

/** A preset length in minutes, or `null` for the user's custom length. */
enum class SettleMode(val minutes: Int?) { One(1), Two(2), Custom(null) }

enum class ZazenMode(val minutes: Int?) { Twenty(20), Forty(40), Custom(null) }

enum class AlertMode(val sound: Boolean, val vibration: Boolean) {
    Bell(sound = true, vibration = false),
    Vibrate(sound = false, vibration = true),
    Both(sound = true, vibration = true),
}

data class ZazenSettings(
    val settleMode: SettleMode = SettleMode.Two,
    val settleCustomMinutes: Int = 5,
    val skipSettle: Boolean = false,
    val zazenMode: ZazenMode = ZazenMode.Forty,
    val zazenCustomMinutes: Int = 30,
    val alert: AlertMode = AlertMode.Bell,
) {
    val settleMinutes: Int get() = settleMode.minutes ?: settleCustomMinutes
    val zazenMinutes: Int get() = zazenMode.minutes ?: zazenCustomMinutes
}

/** Whole minutes in [MIN_MINUTES]..[MAX_MINUTES], or `null` for anything else. */
fun parseMinutes(text: CharSequence): Int? =
    text.trim().toString().toIntOrNull()?.takeIf { it in MIN_MINUTES..MAX_MINUTES }

class ZazenSettingsStore(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<ZazenSettings> = dataStore.data.map { it.toSettings() }

    suspend fun update(transform: (ZazenSettings) -> ZazenSettings) {
        dataStore.edit { preferences ->
            val updated = transform(preferences.toSettings())
            preferences[SETTLE_MODE] = updated.settleMode.name
            preferences[SETTLE_CUSTOM] = updated.settleCustomMinutes
            preferences[SKIP_SETTLE] = updated.skipSettle
            preferences[ZAZEN_MODE] = updated.zazenMode.name
            preferences[ZAZEN_CUSTOM] = updated.zazenCustomMinutes
            preferences[ALERT] = updated.alert.name
        }
    }

    private fun Preferences.toSettings(): ZazenSettings {
        val defaults = ZazenSettings()
        return ZazenSettings(
            settleMode = enumOrDefault(this[SETTLE_MODE], defaults.settleMode),
            settleCustomMinutes = this[SETTLE_CUSTOM] ?: defaults.settleCustomMinutes,
            skipSettle = this[SKIP_SETTLE] ?: defaults.skipSettle,
            zazenMode = enumOrDefault(this[ZAZEN_MODE], defaults.zazenMode),
            zazenCustomMinutes = this[ZAZEN_CUSTOM] ?: defaults.zazenCustomMinutes,
            alert = enumOrDefault(this[ALERT], defaults.alert),
        )
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private companion object {
        val SETTLE_MODE = stringPreferencesKey("settle_mode")
        val SETTLE_CUSTOM = intPreferencesKey("settle_custom_minutes")
        val SKIP_SETTLE = booleanPreferencesKey("skip_settle")
        val ZAZEN_MODE = stringPreferencesKey("zazen_mode")
        val ZAZEN_CUSTOM = intPreferencesKey("zazen_custom_minutes")
        val ALERT = stringPreferencesKey("alert")
    }
}
