package online.mpscan.app.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

object UserDirectory {
 private val lock=Mutex();private var cached=emptyList<ProfilePerson>();private var loadedAt=0L;private var account=""
 suspend fun people(uid:String):List<ProfilePerson> = lock.withLock {
  if(account==uid&&System.currentTimeMillis()-loadedAt<120000)return@withLock cached
  val profiles=SiteAccess.json("perfisPublicos")
  cached=profiles.keys().asSequence().mapNotNull{id->profiles.optJSONObject(id)?.let{ProfileIdentity.person(id,it)}}.toList()
  account=uid;loadedAt=System.currentTimeMillis();cached
 }
 fun matches(person:ProfilePerson,query:String):Boolean {
  val q=query.trim().removePrefix("@").lowercase();if(q.isBlank())return false
  return if(query.trim().startsWith("@"))person.username.removePrefix("@").lowercase().contains(q)else person.name.lowercase().contains(q)||person.username.lowercase().contains(q)
 }
 suspend fun profile(uid:String):JSONObject=SiteAccess.json("perfisPublicos/$uid")
}
