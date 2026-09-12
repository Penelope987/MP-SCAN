package online.mpscan.app.data

import android.content.Context

class FavoritesStore(context:Context){
    private val preferences=context.getSharedPreferences("mp_scan_library",Context.MODE_PRIVATE)
    fun ids():Set<String> = preferences.getStringSet("favorites",emptySet())?.toSet()?:emptySet()
    fun contains(workId:String)=workId in ids()
    fun toggle(workId:String):Boolean{val updated=ids().toMutableSet();val added=if(workId in updated){updated.remove(workId);false}else{updated.add(workId);true};preferences.edit().putStringSet("favorites",updated).apply();return added}
}
