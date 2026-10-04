package online.mpscan.app.data
import org.json.JSONObject

object ChapterMetadata {
 fun millis(value:Long)=if(value in 1..99_999_999_999L)value*1000 else value
 fun latest(chapter:Chapter)=maxOf(millis(chapter.updatedAt),millis(chapter.createdAt),millis(chapter.scheduledAt))
 fun isNew(chapter:Chapter,now:Long=System.currentTimeMillis()):Boolean=chapter.available&&latest(chapter)>0&&now-latest(chapter) in 0..14L*24*60*60*1000
 fun parse(id:String,value:JSONObject):Chapter {
  fun text(vararg keys:String)=keys.firstNotNullOfOrNull{value.optString(it).takeIf{s->s.isNotBlank()&&s!="null"}}?:""
  fun number(vararg keys:String)=keys.firstNotNullOfOrNull{value.opt(it)?.toString()?.toLongOrNull()}?:0L
  val scheduled=millis(number("agendadoPara","scheduledAt"))
  val mode=text("modoPublicacao","publicationMode").ifBlank{when{value.optBoolean("rascunho")->"draft";scheduled>0->"scheduled";!value.optBoolean("publicado",true)||!value.optBoolean("published",true)->"draft";else->"published"}}
  return Chapter(id,text("numero","number").replace(',','.').toDoubleOrNull(),text("titulo","title"),mode!="draft"&&(mode!="scheduled"||scheduled>0),millis(number("atualizadoEm","updatedAt")),millis(number("criadoEm","createdAt")),mode,scheduled)
 }
}
