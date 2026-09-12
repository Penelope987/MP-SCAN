package online.mpscan.app.data

import android.content.Context
import org.json.JSONObject

data class ReadingProgress(
    val workId:String,val workTitle:String,val workCover:String,
    val chapterId:String,val chapterLabel:String,
    val page:Int,val totalPages:Int,val updatedAt:Long
){val percent:Int get()=if(totalPages<=0)0 else ((page.coerceAtMost(totalPages)*100)/totalPages)}

class ReadingStore(context:Context){
    private val preferences=context.getSharedPreferences("mp_scan_reading",Context.MODE_PRIVATE)

    fun progress(workId:String,chapterId:String):ReadingProgress?=
        preferences.getString(key(workId,chapterId),null)?.let(::decode)

    fun save(work:Work,chapter:Chapter,page:Int,totalPages:Int){
        val value=JSONObject().put("workId",work.id).put("workTitle",work.title)
            .put("workCover",work.cover).put("chapterId",chapter.id)
            .put("chapterLabel",chapter.label).put("page",page.coerceAtLeast(1))
            .put("totalPages",totalPages).put("updatedAt",System.currentTimeMillis())
        preferences.edit().putString(key(work.id,chapter.id),value.toString()).apply()
    }

    fun history():List<ReadingProgress> = preferences.all.values.mapNotNull{(it as? String)?.let(::decode)}
        .sortedByDescending{it.updatedAt}

    private fun decode(raw:String)=runCatching{JSONObject(raw).let{ReadingProgress(
        it.getString("workId"),it.optString("workTitle","Obra"),it.optString("workCover"),
        it.getString("chapterId"),it.optString("chapterLabel","Capítulo"),
        it.optInt("page",1),it.optInt("totalPages",0),it.optLong("updatedAt",0)
    )}}.getOrNull()
    private fun key(workId:String,chapterId:String)="progress_${workId}_${chapterId}"
}
