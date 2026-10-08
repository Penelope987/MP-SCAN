package online.mpscan.app.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import org.json.JSONObject

object UserDirectory {

 private val lock=Mutex();private val cache=linkedMapOf<String,Pair<Long,List<ProfilePerson>>>()
 suspend fun search(uid:String,query:String):List<ProfilePerson> {
  val term=query.trim().removePrefix("@");if(term.length<2)return emptyList()
  val key=uid+"|"+query.trim()
  lock.withLock{cache[key]?.takeIf{System.currentTimeMillis()-it.first<60000}?.let{return it.second}}
  fun encode(value:String)=java.net.URLEncoder.encode(JSONObject.quote(value),"UTF-8")
  val variants=if(query.trim().startsWith("@"))listOf("nomeUsuario" to term.lowercase())else (listOf("nomeUsuario" to term.lowercase())+listOf(term,term.lowercase(),term.replaceFirstChar{it.uppercase()}).distinct().map{"nome" to it})
  val result=kotlinx.coroutines.coroutineScope{variants.map{(field,prefix)->async{
   val profiles=SiteAccess.json("perfisPublicos","?orderBy="+encode(field)+"&startAt="+encode(prefix)+"&endAt="+encode(prefix+"\uf8ff")+"&limitToFirst=50")
   profiles.keys().asSequence().mapNotNull{id->profiles.optJSONObject(id)?.let{ProfileIdentity.person(id,it)}}.toList()
  }}.map{it.await()}.flatten().distinctBy{it.uid}}
  lock.withLock{cache[key]=System.currentTimeMillis() to result;while(cache.size>30)cache.remove(cache.keys.first())}
  return result
 }
 fun matches(person:ProfilePerson,query:String):Boolean {
  val q=query.trim().removePrefix("@").lowercase();if(q.isBlank())return false
  return if(query.trim().startsWith("@"))person.username.removePrefix("@").lowercase().contains(q)else person.name.lowercase().contains(q)||person.username.lowercase().contains(q)
 }
 suspend fun profile(uid:String):JSONObject=SiteAccess.json("perfisPublicos/$uid")
 suspend fun viewerProfile(uid:String):JSONObject {
  val public=profile(uid)
  if(public.length()>0&&!public.optBoolean("publico",false)){
   try{val details=SiteAccess.json("perfisPrivados/$uid");if(details.length()>0){details.keys().forEach{key->public.put(key,details.opt(key))};public.put("_privateAllowed",true)}}
   catch(e:CancellationException){throw e}catch(e:Exception){}
  }
  return public
 }

}
