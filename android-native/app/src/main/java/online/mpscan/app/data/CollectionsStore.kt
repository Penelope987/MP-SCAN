package online.mpscan.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class CollectionsStore(context:Context){
    private val preferences=context.getSharedPreferences("mp_scan_library",Context.MODE_PRIVATE)
    private fun root()=runCatching{JSONObject(preferences.getString("collections","{}")?:"{}")}.getOrElse{JSONObject()}
    private fun save(root:JSONObject)=preferences.edit().putString("collections",root.toString()).apply()
    fun names():List<String>{val root=root();return root.keys().asSequence().toList().sorted()}
    fun works(name:String):Set<String>{val array=root().optJSONArray(name)?:JSONArray();return (0 until array.length()).mapNotNull{array.optString(it).takeIf(String::isNotBlank)}.toSet()}
    fun create(name:String):Boolean{val clean=name.trim();if(clean.isBlank())return false;val root=root();if(root.has(clean))return false;root.put(clean,JSONArray());save(root);return true}
    fun rename(old:String,new:String):Boolean{val clean=new.trim();val root=root();if(clean.isBlank()||root.has(clean)||!root.has(old))return false;root.put(clean,root.getJSONArray(old));root.remove(old);save(root);return true}
    fun delete(name:String){val root=root();root.remove(name);save(root)}
    fun toggle(name:String,workId:String):Boolean{val root=root();val current=works(name).toMutableSet();val added=if(workId in current){current.remove(workId);false}else{current.add(workId);true};root.put(name,JSONArray(current.toList()));save(root);return added}
}
