package online.mpscan.app.data
import java.text.Normalizer
import java.util.Locale
import org.json.JSONObject

data class RankedWork(val work:Work,val average:Double,val votes:Int)
object Discovery {
 fun matches(work:Work,query:String):Boolean{
  fun normalize(value:String)=Normalizer.normalize(value,Normalizer.Form.NFD).replace(Regex("\\p{M}"),"").lowercase(Locale.ROOT)
  val text=normalize(listOf(work.title,work.alternateTitle,work.synopsis,work.author,work.artist,work.scan,work.genres.joinToString(" ")).joinToString(" "))
  return normalize(query).trim().split(Regex("\\s+")).all{text.contains(it)}
 }
 fun genresMatch(work:Work,selected:Set<String>)=selected.all{genre->work.genres.any{it.equals(genre,true)}}
 fun ranking(works:List<Work>,ratings:JSONObject):List<RankedWork> = works.mapNotNull{work->
  val votes=ratings.optJSONObject(work.id)?:return@mapNotNull null
  val notes=votes.keys().asSequence().mapNotNull{uid->votes.optJSONObject(uid)?.optInt("nota")?.takeIf{it in 1..5}}.toList()
  if(notes.isEmpty())null else RankedWork(work,notes.average(),notes.size)
 }.sortedWith(compareByDescending<RankedWork>{it.votes}.thenByDescending{it.average}.thenBy{it.work.title})
}
