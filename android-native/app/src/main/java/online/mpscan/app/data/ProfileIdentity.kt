package online.mpscan.app.data
import org.json.JSONObject
object ProfileIdentity {
 fun person(uid:String,vararg sources:JSONObject):ProfilePerson {
  fun field(vararg keys:String):String=sources.firstNotNullOfOrNull{source->keys.firstNotNullOfOrNull{key->source.optString(key).takeIf{it.isNotBlank()&&it!="null"}}}.orEmpty()
  val handle=field("nomeUsuario","username","arroba")
  return ProfilePerson(uid,field("nome","name").ifBlank{handle.ifBlank{"Perfil indisponível"}},handle,field("foto","photo","photoURL"))
 }
}
object ProfileSnapshots {
 private val values=java.util.concurrent.ConcurrentHashMap<String,AccountProfile>()
 fun get(uid:String?)=uid?.let{values[it]}
 fun save(profile:AccountProfile){values[profile.uid]=profile}
}
