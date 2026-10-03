package online.mpscan.app.data
object DiscoveryPolicy {
 fun visible(adult:Boolean,mode:String)=!adult||mode!="hide"
 fun matchesGenres(actual:List<String>,selected:Set<String>,all:Boolean=false):Boolean {
  if(selected.isEmpty())return true
  val has:(String)->Boolean={g->actual.any{it.equals(g,true)}}
  return if(all)selected.all(has)else selected.any(has)
 }
 fun eligible(work:Work,mode:String)=visible(work.adult,mode)&&work.cover.isNotBlank()&&work.status.lowercase() !in setOf("future","futura","futuro")
}
