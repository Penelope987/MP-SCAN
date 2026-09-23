package online.mpscan.app.data

import android.content.Context

class WorkSubscriptionsStore(context: Context) {
    private val preferences = context.getSharedPreferences("mp_scan_notifications", Context.MODE_PRIVATE)
    fun contains(workId: String) = workId in ids()
    fun toggle(workId: String): Boolean {
        val updated = ids().toMutableSet()
        val enabled = if (workId in updated) { updated.remove(workId); false } else { updated.add(workId); true }
        preferences.edit().putStringSet("new_chapter_works", updated).apply()
        return enabled
    }
    fun ids() = preferences.getStringSet("new_chapter_works", emptySet())?.toSet() ?: emptySet()
    fun lastSeen(workId: String) = preferences.getLong("last_$workId", 0L)
    fun setLastSeen(workId: String, value: Long) = preferences.edit().putLong("last_$workId", value).apply()
}
