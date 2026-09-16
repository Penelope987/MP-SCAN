package online.mpscan.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class WorkSocialRepository(private val base:String="https://nnnsss-23f2f-default-rtdb.firebaseio.com"){
 private fun e(value:String)=URLEncoder.encode(value,"UTF-8")
 suspend fun rating(workId:String,session:AccountSession?):WorkRating=withContext(Dispatchers.IO){
  val root=request("$base/ratings/${e(workId)}.json");val counts=(1..5).associateWith{0}.toMutableMap();var sum=0;var total=0;var mine=0
  root.keys().forEach{uid->root.optJSONObject(uid)?.optInt("nota")?.takeIf{it in 1..5}?.let{note->counts[note]=(counts[note]?:0)+1;sum+=note;total++;if(uid==session?.uid)mine=note}}
  WorkRating(if(total==0)0.0 else sum.toDouble()/total,total,counts,mine)
 }
 suspend fun rate(workId:String,note:Int,session:AccountSession)=withContext(Dispatchers.IO){
  request("$base/ratings/${e(workId)}/${e(session.uid)}.json?auth=${e(session.token)}","PUT",JSONObject().put("nota",note.coerceIn(1,5)).put("data",System.currentTimeMillis()).toString())
 }
 suspend fun reactions(workId:String,session:AccountSession?):List<WorkReaction> = withContext(Dispatchers.IO){
  val specific=request("$base/contentReactions/definitions/work/${e(workId)}.json");val global=request("$base/contentReactions/definitions/global/work.json");val votes=request("$base/contentReactions/votes/work/${e(workId)}.json")
  val counts=mutableMapOf<String,Int>();votes.keys().forEach{uid->votes.optJSONObject(uid)?.let{v->val source=if(v.optString("source")=="global")"global" else "specific";val key="$source:${v.optString("reactionId")}";counts[key]=(counts[key]?:0)+1}}
  val mine=session?.uid?.let{votes.optJSONObject(it)};val mineSource=if(mine?.optString("source")=="global")"global" else "specific"
  fun read(root:JSONObject,source:String)=root.keys().asSequence().mapNotNull{id->root.optJSONObject(id)?.takeIf{it.optBoolean("active",true)}?.let{r->val key="$source:$id";WorkReaction(id,r.optString("label","Reação"),r.optString("imageUrl"),source,counts[key]?:0,mine?.optString("reactionId")==id&&mineSource==source)}}.toList()
  read(specific,"specific")+read(global,"global")
 }
 suspend fun react(workId:String,reaction:WorkReaction,session:AccountSession)=withContext(Dispatchers.IO){
  request("$base/contentReactions/votes/work/${e(workId)}/${e(session.uid)}.json?auth=${e(session.token)}","PUT",JSONObject().put("uid",session.uid).put("reactionId",reaction.id).put("source",reaction.source).put("data",System.currentTimeMillis()).toString())
 }
 private fun request(url:String,method:String="GET",body:String?=null):JSONObject{val c=URL(url).openConnection() as HttpURLConnection;c.requestMethod=method;c.connectTimeout=15000;c.readTimeout=25000;c.setRequestProperty("Content-Type","application/json");if(body!=null){c.doOutput=true;c.outputStream.use{it.write(body.toByteArray())}};val ok=c.responseCode in 200..299;val text=(if(ok)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty();c.disconnect();if(!ok)error("Não foi possível concluir a operação.");return if(text.isBlank()||text=="null")JSONObject()else JSONObject(text)}
}
