package online.mpscan.app.data

import org.json.JSONArray
import org.json.JSONObject

object OfflineMetadata {
 fun encode(work:Work)=JSONObject().put("id",work.id).put("title",work.title).put("synopsis",work.synopsis).put("cover",work.cover).put("banner",work.banner).put("type",work.type).put("status",work.status).put("author",work.author).put("genres",JSONArray(work.genres)).put("updatedAt",work.updatedAt).put("reads",work.reads).put("alternateTitle",work.alternateTitle).put("artist",work.artist).put("year",work.year).put("scan",work.scan).put("hosting",work.hosting).put("language",work.language).put("schedule",work.schedule).put("scanOwnerUid",work.scanOwnerUid)
 fun decode(value:JSONObject):Work {
  val genres=value.optJSONArray("genres")?:JSONArray()
  return Work(value.getString("id"),value.optString("title"),value.optString("synopsis"),value.optString("cover"),value.optString("banner"),value.optString("type"),value.optString("status"),value.optString("author"),(0 until genres.length()).map{genres.getString(it)},value.optLong("updatedAt"),value.optLong("reads"),value.optString("alternateTitle"),value.optString("artist"),value.optString("year"),value.optString("scan"),value.optString("hosting"),value.optString("language","Português"),value.optString("schedule","Sem dia fixo"),value.optString("scanOwnerUid"))
 }
 fun encode(chapter:Chapter)=JSONObject().put("id",chapter.id).put("number",chapter.number?:JSONObject.NULL).put("title",chapter.title).put("updatedAt",chapter.updatedAt).put("createdAt",chapter.createdAt)
 fun chapter(value:JSONObject)=Chapter(value.getString("id"),if(value.isNull("number"))null else value.optDouble("number").takeIf{it.isFinite()},value.optString("title"),true,value.optLong("updatedAt"),value.optLong("createdAt"))
}
