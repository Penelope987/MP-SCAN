package online.mpscan.app.data
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.*

data class AccountSession(val uid:String,val email:String,val token:String,val refreshToken:String="")
data class CommentFrame(val id:String,val name:String,val image:String,val color:String,val background:String,val active:Boolean,val owned:Boolean,val exclusiveToUid:String="")
data class ProfilePerson(val uid:String,val name:String,val username:String,val photo:String)
data class ProfileWork(val id:String,val title:String,val cover:String)
data class ProfileCollection(val id:String,val name:String,val works:List<ProfileWork>)
data class ProfileActivity(val type:String,val title:String,val detail:String,val date:Long)
data class ProfileExtras(val followers:List<ProfilePerson>,val following:List<ProfilePerson>,val favorites:List<ProfileWork>,val collections:List<ProfileCollection>,val activities:List<ProfileActivity>)
data class AccountProfile(val uid:String,val name:String,val username:String,val bio:String,val photo:String,val cover:String,val color:String,val isPublic:Boolean,val frameId:String,val role:String,val followers:Int=0,val following:Int=0,val comments:Int=0)
class AccountStore(c:Context){private val p=c.getSharedPreferences("mp_account",Context.MODE_PRIVATE);fun session():AccountSession?{val u=p.getString("uid","").orEmpty();val t=p.getString("token","").orEmpty();return if(u.isBlank()||t.isBlank())null else AccountSession(u,p.getString("email","").orEmpty(),t,p.getString("refresh_token","").orEmpty())};fun save(s:AccountSession){p.edit().putString("uid",s.uid).putString("email",s.email).putString("token",s.token).putString("refresh_token",s.refreshToken).apply()};fun clear(){p.edit().clear().apply()}}
class AccountRepository{
 private val base="https://nnnsss-23f2f-default-rtdb.firebaseio.com";private val key="AIzaSyAbpqQIxWuEnFolv3lNjNDoPKTGm0mtrxU";private fun e(v:String)=URLEncoder.encode(v,"UTF-8")
 suspend fun signIn(email:String,password:String)=withContext(Dispatchers.IO){val x=req("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$key","POST",JSONObject().put("email",email.trim()).put("password",password).put("returnSecureToken",true).toString());AccountSession(x.getString("localId"),x.optString("email",email),x.getString("idToken"),x.optString("refreshToken"))}
 suspend fun refresh(s:AccountSession)=withContext(Dispatchers.IO){
  if(s.refreshToken.isBlank())return@withContext s
  val body="grant_type=refresh_token&refresh_token="+e(s.refreshToken)
  val c=URL("https://securetoken.googleapis.com/v1/token?key=$key").openConnection() as HttpURLConnection
  c.requestMethod="POST";c.connectTimeout=15000;c.readTimeout=25000;c.doOutput=true;c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");c.outputStream.use{it.write(body.toByteArray())}
  val ok=c.responseCode in 200..299;val text=(if(ok)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty();c.disconnect()
  if(!ok)error("Sua sessão expirou. Entre novamente na conta.")
  val x=JSONObject(text);AccountSession(x.optString("user_id",s.uid),s.email,x.getString("id_token"),x.optString("refresh_token",s.refreshToken))
 }
 suspend fun profile(s:AccountSession)=withContext(Dispatchers.IO){
  val u=req("$base/usuarios/${e(s.uid)}.json?auth=${e(s.token)}")
  val pub=runCatching{req("$base/perfisPublicos/${e(s.uid)}.json?auth=${e(s.token)}")}.getOrDefault(JSONObject())
  fun value(vararg keys:String):String{for(k in keys){u.optString(k).takeIf{it.isNotBlank()}?.let{return it};pub.optString(k).takeIf{it.isNotBlank()}?.let{return it}};return ""}
  val followers=count("$base/seguidores/${e(s.uid)}.json?auth=${e(s.token)}");val following=count("$base/seguindo/${e(s.uid)}.json?auth=${e(s.token)}")
  AccountProfile(s.uid,value("nome","name").ifBlank{"Leitor MP SCAN"},value("nomeUsuario","username"),value("bio"),value("foto","photo"),value("capaPerfil","cover"),value("corPerfil","color").ifBlank{"#8d2bff"},if(u.has("publico"))u.optBoolean("publico") else pub.optBoolean("publico",true),value("molduraComentarioId"),if(u.optBoolean("admin"))"ADM" else value("papel","role").ifBlank{"Usuário"},followers,following,countOwn(s.uid))
 }
 suspend fun frames(s:AccountSession)=withContext(Dispatchers.IO){
  val auth="?auth=${e(s.token)}"
  val defs=req("$base/config/commentFrames.json")
  val inv=runCatching{req("$base/commentFrameInventory/${e(s.uid)}.json$auth")}.getOrDefault(JSONObject())
  fun scalar(path:String)=runCatching{req("$base/$path.json$auth").optString("value")}.getOrDefault("")
  val selected=setOf(
   scalar("usuarios/${e(s.uid)}/molduraComentarioId"),
   scalar("identidadesComentarios/${e(s.uid)}/molduraComentarioId"),
   scalar("perfisPublicos/${e(s.uid)}/molduraComentarioId")
  ).filter{it.isNotBlank()}.toSet()
  val ownedIds=mutableSetOf<String>()
  fun collect(x:JSONObject){
   x.keys().forEach{k->
    val value=x.opt(k)
    if(value!=false&&value!=JSONObject.NULL)ownedIds+=k
    when(value){
     is JSONObject->{
      listOf("id","frameId","molduraId","molduraComentarioId").forEach{field->value.optString(field).takeIf{it.isNotBlank()}?.let(ownedIds::add)}
      collect(value)
     }
     is String->if(value.isNotBlank())ownedIds+=value
    }
   }
  }
  collect(inv);ownedIds+=selected
  defs.keys().asSequence().mapNotNull{id->defs.optJSONObject(id)?.let{f->
   fun v(vararg ks:String):String{ks.forEach{k->f.optString(k).takeIf{it.isNotBlank()}?.let{return it}};return ""}
   val definitionId=v("id","frameId","molduraId")
   val active=!f.has("ativo")||f.optBoolean("ativo")||f.optString("ativo").equals("true",true)
   val owned=id in ownedIds||definitionId in ownedIds
   val exclusive=v("exclusiveToUid")
   if(active&&(exclusive.isBlank()||exclusive==s.uid))CommentFrame(id,v("nome","name").ifBlank{"Moldura MP SCAN"},v("imageUrl","imagemUrl","imagem","backgroundImageUrl","backgroundImage","fundoImagem","fundoUrl","url","previewUrl"),v("borderColor","bordaCor","corBorda").ifBlank{"#8d2bff"},v("bgColor","fundoCor","backgroundColor","corFundo").ifBlank{"#17171d"},active,owned,exclusive)else null
  }}.toList()
 }
 suspend fun claimFrame(s:AccountSession,p:AccountProfile,frame:CommentFrame)=withContext(Dispatchers.IO){
  if(!frame.active)error("Esta moldura não está ativa.")
  if(frame.exclusiveToUid.isNotBlank()&&frame.exclusiveToUid!=s.uid)error("Esta moldura é exclusiva de outra conta.")
  val auth="?auth=${e(s.token)}"
  val definition=req("$base/config/commentFrames/${e(frame.id)}.json")
  if(definition.length()==0||definition.optString("ativo").equals("false",true))error("Esta moldura não está disponível.")
  val exclusive=definition.optString("exclusiveToUid")
  if(exclusive.isNotBlank()&&exclusive!=s.uid)error("Esta moldura é exclusiva de outra conta.")
  req("$base/commentFrameInventory/${e(s.uid)}/${e(frame.id)}.json$auth","PUT",JSONObject().put("data",System.currentTimeMillis()).put("origem","app").toString())
  selectFrame(s,p,frame.id)
 }
 suspend fun clearFrame(s:AccountSession,p:AccountProfile)=selectFrame(s,p,"")
 suspend fun selectFrame(s:AccountSession,p:AccountProfile,frameId:String)=withContext(Dispatchers.IO){
  val id=frameId.trim();val auth="?auth=${e(s.token)}"
  if(id.isNotBlank()){
   val definition=req("$base/config/commentFrames/${e(id)}.json")
   if(definition.length()==0||definition.optString("ativo").equals("false",true)||(definition.has("ativo")&&!definition.optBoolean("ativo")&&!definition.optString("ativo").equals("true",true)))error("Esta moldura não está ativa.")
   val inventory=req("$base/commentFrameInventory/${e(s.uid)}/${e(id)}.json$auth")
   if(inventory.length()==0)error("Esta moldura não está liberada para esta conta.")
  }
  req("$base/usuarios/${e(s.uid)}/molduraComentarioId.json$auth","PUT",JSONObject.quote(id))
  req("$base/identidadesComentarios/${e(s.uid)}.json$auth","PUT",JSONObject().put("uid",s.uid).put("nome",p.name).put("nomeUsuario",p.username.removePrefix("@")).put("foto",p.photo).put("molduraComentarioId",id).toString())
  runCatching{req("$base/perfisPublicos/${e(s.uid)}/molduraComentarioId.json$auth","PUT",JSONObject.quote(id))}
 }
 suspend fun extras(s:AccountSession)=withContext(Dispatchers.IO){
  val auth="?auth=${e(s.token)}";val profiles=runCatching{req("$base/perfisPublicos.json$auth")}.getOrDefault(JSONObject());val works=runCatching{req("$base/obras.json")}.getOrDefault(JSONObject())
  fun person(uid:String):ProfilePerson{val p=profiles.optJSONObject(uid)?:JSONObject();return ProfilePerson(uid,p.optString("nome","Leitor MP SCAN"),p.optString("nomeUsuario"),p.optString("foto"))}
  fun work(id:String):ProfileWork{val w=works.optJSONObject(id)?:JSONObject();return ProfileWork(id,w.optString("titulo",w.optString("nome","Obra")),w.optString("capa",w.optString("cover")))}
  val followerRoot=runCatching{req("$base/seguidores/${e(s.uid)}.json$auth")}.getOrDefault(JSONObject());val followingRoot=runCatching{req("$base/seguindo/${e(s.uid)}.json$auth")}.getOrDefault(JSONObject())
  val favRoot=runCatching{req("$base/favoritos/${e(s.uid)}.json$auth")}.getOrDefault(JSONObject());val favorites=favRoot.keys().asSequence().filter{id->val v=favRoot.opt(id);v!=false&&v!=JSONObject.NULL}.map(::work).toList()
  val colRoot=runCatching{req("$base/colecoes/${e(s.uid)}.json$auth")}.getOrDefault(JSONObject());val collections=colRoot.keys().asSequence().mapNotNull{id->colRoot.optJSONObject(id)?.let{c->val wr=c.optJSONObject("obras")?:c.optJSONObject("items")?:JSONObject();ProfileCollection(id,c.optString("nome",c.optString("name","Coleção")),wr.keys().asSequence().map(::work).toList())}}.toList()
  val activities=mutableListOf<ProfileActivity>();val comments=runCatching{req("$base/comentariosV1.json")}.getOrDefault(JSONObject())
  fun scanComments(x:JSONObject){x.keys().forEach{k->x.optJSONObject(k)?.let{v->if(v.optString("uid")==s.uid&&v.has("texto"))activities+=ProfileActivity("comentario","Comentário",v.optString("texto"),v.optLong("data"));scanComments(v)}}};scanComments(comments)
  val ratings=runCatching{req("$base/ratings.json")}.getOrDefault(JSONObject());ratings.keys().forEach{wid->ratings.optJSONObject(wid)?.optJSONObject(s.uid)?.let{r->activities+=ProfileActivity("avaliacao",work(wid).title,"${r.optInt("nota")}/5",r.optLong("data"))}}
  ProfileExtras(followerRoot.keys().asSequence().map(::person).toList(),followingRoot.keys().asSequence().map(::person).toList(),favorites,collections,activities.sortedByDescending{it.date})
 }
 suspend fun saveProfile(s:AccountSession,p:AccountProfile)=withContext(Dispatchers.IO){val x=JSONObject().put("nome",p.name).put("nomeUsuario",p.username.removePrefix("@")).put("bio",p.bio).put("foto",p.photo).put("capaPerfil",p.cover).put("corPerfil",p.color).put("publico",p.isPublic).put("molduraComentarioId",p.frameId).put("atualizadoEm",System.currentTimeMillis());req("$base/usuarios/${e(s.uid)}.json?auth=${e(s.token)}","PATCH",x.toString());val pub=JSONObject(x.toString()).put("uid",s.uid);if(!p.isPublic){pub.remove("bio");pub.remove("capaPerfil")};req("$base/perfisPublicos/${e(s.uid)}.json?auth=${e(s.token)}","PUT",pub.toString());req("$base/identidadesComentarios/${e(s.uid)}.json?auth=${e(s.token)}","PUT",JSONObject().put("uid",s.uid).put("nome",p.name).put("nomeUsuario",p.username.removePrefix("@")).put("foto",p.photo).put("molduraComentarioId",p.frameId).toString())}
 private fun count(url:String)=runCatching{req(url).length()}.getOrDefault(0)
 private fun countOwn(uid:String):Int{val root=runCatching{req("$base/comentariosV1.json")}.getOrDefault(JSONObject());var n=0;fun walk(x:JSONObject){x.keys().forEach{k->x.optJSONObject(k)?.let{v->if(v.optString("uid")==uid)n++;walk(v)}}};walk(root);return n}
 fun request(url:String,m:String="GET",body:String?=null)=req(url,m,body)
 private fun req(url:String,m:String="GET",body:String?=null):JSONObject{val c=URL(url).openConnection() as HttpURLConnection;c.requestMethod=m;c.connectTimeout=15000;c.readTimeout=25000;c.setRequestProperty("Content-Type","application/json");if(body!=null){c.doOutput=true;c.outputStream.use{it.write(body.toByteArray())}};val ok=c.responseCode in 200..299;val t=(if(ok)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty();c.disconnect();if(!ok)error(if("INVALID_PASSWORD" in t||"EMAIL_NOT_FOUND" in t)"E-mail ou senha incorretos." else if("TOKEN_EXPIRED" in t)"Sua sessão expirou. Entre novamente." else "Não foi possível conectar.");return if(t.isBlank()||t=="null")JSONObject()else runCatching{JSONObject(t)}.getOrElse{JSONObject().put("value",t.trim('"'))}}
}
