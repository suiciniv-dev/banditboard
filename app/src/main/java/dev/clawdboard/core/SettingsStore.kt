package dev.clawdboard.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class SettingsStore(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _flow = MutableStateFlow(read().also { I18n.language = it.language })
    val flow: StateFlow<Prefs> = _flow.asStateFlow()
    val value: Prefs get() = _flow.value

    private fun read(): Prefs {
        val raw = sp.getString("prefs", null) ?: return Prefs()
        return runCatching { Prefs().merge(JSONObject(raw)) }.getOrDefault(Prefs())
    }

    @Synchronized
    fun update(f: (Prefs) -> Prefs): Prefs {
        val p = f(_flow.value).sanitized()
        sp.edit().putString("prefs", p.toJson().toString()).apply()
        I18n.language = p.language
        _flow.value = p
        return p
    }

    @Synchronized
    fun clear() {
        sp.edit().clear().apply()
        I18n.language = Language.AUTO
        _flow.value = Prefs()
    }
}
