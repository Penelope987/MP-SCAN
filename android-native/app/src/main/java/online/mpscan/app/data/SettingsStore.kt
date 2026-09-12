package online.mpscan.app.data

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("mp_scan_settings", Context.MODE_PRIVATE)
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
