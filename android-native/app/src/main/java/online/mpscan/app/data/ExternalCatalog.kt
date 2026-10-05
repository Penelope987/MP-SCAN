package online.mpscan.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject
import org.json.JSONArray
import java.net.URL
import java.net.HttpURLConnection
import java.io.IOException

 data class ScanPartnership(val id:String,val name:String,val url:String,val cover:String="",val photo:String="",val handle:String="",val description:String="",val enabled:Boolean=true,val ownerUid:String="",val responsibleUid:String="",val draft:Boolean=false){
 fun json()=JSONObject().put("name",name).put("url",url).put("cover",cover).put("photo",photo).put("handle",handle).put("description",description).put("enabled",enabled).put("ownerUid",ownerUid).put("responsibleUid",responsibleUid).put("publicationMode",if(draft)"draft"else"published")
 companion object {fun parse(id:String,x:JSONObject)=ScanPartnership(id,x.optString("name"),x.optString("url"),x.optString("cover"),x.optString("photo"),x.optString("handle"),x.optString("description"),x.optBoolean("enabled",true),x.optString("ownerUid"),x.optString("responsibleUid"),x.optString("publicationMode")=="draft")}
 }
object ExternalCatalog {
 const val CONFIG="config/externalScanPartners"
 val example=ScanPartnership("kenji-example","Kenji Traduções","https://nocfsb.com/manga-tag/kenji-traducoes/",description="Somente as obras da página indicada. Integração em teste no aparelho.")
 private lateinit var prefs:SharedPreferences
 fun init(context:Context){prefs=context.applicationContext.getSharedPreferences("mp_external_catalog",0)}
 fun isExternal(id:String)=id.startsWith("ext_")
 private fun read(key:String)=runCatching{JSONObject(prefs.getString(key,"{}")?:"{}")}.getOrDefault(JSONObject())
 private fun put(key:String,value:JSONObject){prefs.edit().putString(key,value.toString()).apply()}
 suspend fun partners(context:Context):List<ScanPartnership> {
  val uid=AccountStore(context).session()?.uid.orEmpty()
  val root=try{SiteAccess.json(CONFIG).also{put("partners",it)}}catch(e:CancellationException){throw e}catch(e:Exception){read("partners")}
  val published=root.keys().asSequence().mapNotNull{id->root.optJSONObject(id)?.let{ScanPartnership.parse(id,it)}}.filter{it.enabled&&!it.draft}.toList()
  if(uid.isBlank())return published.sortedBy{it.name}
  val drafts=try{SiteAccess.json("config/externalScanDrafts/$uid").also{put("drafts_$uid",it)}}catch(e:CancellationException){throw e}catch(e:Exception){read("drafts_$uid")}
  val personal=drafts.keys().asSequence().mapNotNull{id->drafts.optJSONObject(id)?.let{ScanPartnership.parse(id,it)}}.filter{it.ownerUid==uid&&it.draft}.toList()
  return (published+personal).distinctBy{it.id}.sortedBy{it.name}
 }
 private suspend fun admin(context:Context):AccountSession {
  val store=AccountStore(context);val old=store.session()?:throw ExternalSourceException("Entre com uma conta ADM para gerenciar parcerias.")
  val fresh=AccountRepository().refresh(old);store.save(fresh)
  val profile=AccountRepository().profile(fresh)
  check(profile.role.lowercase() in listOf("adm","administrador","admin")){"Sua conta não tem permissão para gerenciar parcerias."}
  SiteAccess.requireAllowed(context);return fresh
 }
 suspend fun save(context:Context,partner:ScanPartnership){
  require(partner.name.trim().isNotBlank()){ "Informe o nome da scan." };ExternalSourceParser.url(partner.url)
  val fresh=admin(context);val value=partner.copy(ownerUid=fresh.uid,enabled=true)
  val patch=JSONObject().put("$CONFIG/${value.id}",if(value.draft)JSONObject.NULL else value.json())
   .put("config/externalScanDrafts/${fresh.uid}/${value.id}",if(value.draft)value.json()else JSONObject.NULL)
  mutate(patch)
  val public=read("partners");if(value.draft)public.remove(value.id)else public.put(value.id,value.json());put("partners",public)
  val drafts=read("drafts_${fresh.uid}");if(value.draft)drafts.put(value.id,value.json())else drafts.remove(value.id);put("drafts_${fresh.uid}",drafts)
 }
 suspend fun delete(context:Context,partner:ScanPartnership){
  val fresh=admin(context)
  mutate(JSONObject().put("$CONFIG/${partner.id}",JSONObject.NULL).put("config/externalScanDrafts/${fresh.uid}/${partner.id}",JSONObject.NULL))
  val public=read("partners");public.remove(partner.id);put("partners",public)
  val drafts=read("drafts_${fresh.uid}");drafts.remove(partner.id);put("drafts_${fresh.uid}",drafts)
 }
 private suspend fun mutate(patch:JSONObject)=withContext(Dispatchers.IO){
  try{AccountRepository().request(SiteAccess.authenticated("https://nnnsss-23f2f-default-rtdb.firebaseio.com/.json"),"PATCH",patch.toString())}
  catch(e:CancellationException){throw e}catch(e:Exception){throw ExternalSourceException("Não foi possível salvar a alteração. Confira a conexão e publique as regras de parcerias desta versão.")}
 }
 suspend fun catalog(partner:ScanPartnership):List<Work> = withContext(Dispatchers.IO){
  val scope=ExternalSourceParser.url(partner.url);var next:String?=scope;val visited=mutableSetOf<String>();val result=linkedMapOf<String,Work>()
  while(next!=null){
   currentCoroutineContext().ensureActive();val address=next!!
   if(!visited.add(address))throw ExternalSourceException("A paginação da origem se repetiu. O catálogo anterior foi preservado.")
   if(visited.size>100)throw ExternalSourceException("A origem tem muitas páginas. A equipe precisa preparar uma integração específica.")
   val listing=ExternalSourceParser.listing(fetch(address,scope),address,scope,partner.name,partner.id)
   listing.works.forEach{(work,url)->result[work.id]=work;put("work_${work.id}",JSONObject().put("url",url).put("scope",scope).put("work",OfflineMetadata.encode(work)))}
   next=listing.next
  }
  put("catalog_${partner.id}",JSONObject().put("works",JSONArray(result.values.map{OfflineMetadata.encode(it)})))
  result.values.toList()
 }
 fun cachedCatalog(partnerId:String):List<Work>{val values=read("catalog_$partnerId").optJSONArray("works")?:return emptyList();return (0 until values.length()).mapNotNull{runCatching{OfflineMetadata.decode(values.getJSONObject(it))}.getOrNull()}}
 private fun record(workId:String):JSONObject=read("work_$workId").also{if(it.optString("url").isBlank())throw ExternalSourceException("Não foi possível localizar a origem desta obra. Abra a página da parceria novamente.")}
 suspend fun details(work:Work):Work=withContext(Dispatchers.IO){val value=record(work.id);ExternalSourceParser.details(fetch(value.getString("url"),value.getString("scope")),value.getString("url"),work).also{value.put("work",OfflineMetadata.encode(it));put("work_${work.id}",value)}}
 suspend fun chapters(workId:String):List<Chapter> = withContext(Dispatchers.IO){
  val value=record(workId);val address=value.getString("url");val scope=value.getString("scope");val html=fetch(address,scope)
  var chapters=ExternalSourceParser.chapters(html,address)
  if(chapters.isEmpty())chapters=ExternalSourceParser.chapters(fetch(address.trimEnd('/')+"/ajax/chapters/",address,"POST"),address)
  if(chapters.isEmpty())throw ExternalSourceException("Não foi possível obter a lista de capítulos da origem. Nenhum download foi iniciado.")
  val root=JSONObject();chapters.forEach{(chapter,url)->root.put(chapter.id,JSONObject().put("url",url).put("chapter",OfflineMetadata.encode(chapter)))}
  value.put("chapters",root);put("work_$workId",value);chapters.map{it.first}
 }
 fun cachedChapters(workId:String):List<Chapter>{val root=read("work_$workId").optJSONObject("chapters")?:return emptyList();return root.keys().asSequence().mapNotNull{id->runCatching{OfflineMetadata.chapter(root.getJSONObject(id).getJSONObject("chapter"))}.getOrNull()}.toList()}
 suspend fun pages(workId:String,chapterId:String):List<String> = withContext(Dispatchers.IO){
  var value=record(workId)
  if(value.optJSONObject("chapters")?.optJSONObject(chapterId)==null){chapters(workId);value=record(workId)}
  val address=value.optJSONObject("chapters")?.optJSONObject(chapterId)?.optString("url").orEmpty()
  if(address.isBlank()||!ExternalSourceParser.sameOrigin(value.getString("scope"),address))throw ExternalSourceException("Este capítulo não pertence à origem da parceria.")
  val listUrl=address+(if('?' in address)"&"else"?")+"style=list"
  ExternalSourceParser.pages(fetch(listUrl,address),listUrl).map{image->if(image.startsWith(ChapterText.PREFIX))image else "mpscan-image:"+JSONObject().put("url",image).put("referer",address).toString()}
 }
 private fun fetch(address:String,referer:String,method:String="GET"):String {
  ExternalSourceParser.url(address)
  var current=address
  repeat(5){
   val c=URL(current).openConnection() as HttpURLConnection;c.connectTimeout=20000;c.readTimeout=30000;c.instanceFollowRedirects=false;c.requestMethod=method
   c.setRequestProperty("User-Agent","MP-SCAN/5.0 Android partner reader");c.setRequestProperty("Referer",referer);c.setRequestProperty("Accept","text/html")
   try{
    val code=c.responseCode
    if(code in listOf(301,302,303,307,308)){val target=URL(URL(current),c.getHeaderField("Location")?:throw ExternalSourceException("A origem redirecionou sem informar o destino." )).toString();if(!ExternalSourceParser.sameOrigin(address,target))throw ExternalSourceException("A origem redirecionou para outro site. O filtro da parceria foi preservado.");current=target}
    else{
     if(code in listOf(401,403,429))throw ExternalSourceException("O site parceiro restringiu o acesso nesta conexão. A equipe precisa liberar a integração.")
     if(code !in 200..299)throw ExternalSourceException("Não foi possível acessar a origem agora. Tente novamente mais tarde.")
     if(c.contentLengthLong>12L*1024*1024)throw ExternalSourceException("A página da origem é grande demais para importar com segurança.")
     val bytes=c.inputStream.use{it.readBytesLimited(12*1024*1024)}
     return bytes.toString(Charsets.UTF_8)
    }
   }finally{c.disconnect()}
  }
  throw ExternalSourceException("A origem redirecionou muitas vezes. Tente novamente mais tarde.")
 }
 private fun java.io.InputStream.readBytesLimited(limit:Int):ByteArray {val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(16384);while(true){val n=read(buffer);if(n<0)break;if(out.size()+n>limit)throw ExternalSourceException("A página da origem excedeu o limite de importação.");out.write(buffer,0,n)};return out.toByteArray()}
}
