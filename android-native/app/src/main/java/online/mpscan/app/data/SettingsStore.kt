package online.mpscan.app.data

import android.content.Context

class SettingsStore(context: Context) {
    val prefs = context.getSharedPreferences("mp_scan_settings", Context.MODE_PRIVATE)
    var adultDisplay: String
        get() = prefs.getString("adult_display", "show").orEmpty().takeIf { it in setOf("show","blur","hide") } ?: "show"
        set(value) { require(value in setOf("show","blur","hide")); prefs.edit().putString("adult_display", value).apply() }
    var discoveryDone: Boolean
        get() = prefs.getBoolean("discovery_done", false)
        set(value) = prefs.edit().putBoolean("discovery_done", value).apply()
    fun choices(): Map<String,String> = runCatching { org.json.JSONObject(prefs.getString("discovery_choices", "{}").orEmpty()).let { x -> x.keys().asSequence().associateWith { x.optString(it) } } }.getOrDefault(emptyMap())
    fun choose(id:String,value:String) { require(value in setOf("like","no","skip")); val updated=org.json.JSONObject(choices()); updated.put(id,value); prefs.edit().putString("discovery_choices",updated.toString()).apply() }
    var lightTheme: Boolean
        get() = prefs.getBoolean("light_theme", false)
        set(value) = prefs.edit().putBoolean("light_theme", value).apply()
    var roseAccent: Boolean
        get() = prefs.getBoolean("rose_accent", false)
        set(value) = prefs.edit().putBoolean("rose_accent", value).apply()
    var notifications: Boolean
        get() = prefs.getBoolean("notifications", true)
        set(value) = prefs.edit().putBoolean("notifications", value).apply()
    var wifiOnly: Boolean
        get() = prefs.getBoolean("wifi_only", true)
        set(value) = prefs.edit().putBoolean("wifi_only", value).apply()
    var animations: Boolean
        get() = prefs.getBoolean("animations", true)
        set(value) = prefs.edit().putBoolean("animations", value).apply()
    var wideReader: Boolean
        get() = prefs.getBoolean("wide_reader", true)
        set(value) = prefs.edit().putBoolean("wide_reader", value).apply()
}
