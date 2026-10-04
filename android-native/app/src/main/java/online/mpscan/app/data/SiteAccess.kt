package online.mpscan.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SiteBan(val active:Boolean=false,val permanent:Boolean=false,val until:Long=0,val reason:String=""){
 fun blocks(now:Long=System.currentTimeMillis())=active&&(permanent||until>now)
 companion object {fun parse(x:JSONObject)=SiteBan(x.optBoolean("active"),x.optBoolean("permanent"),x.optLong("until"),x.optString("reason"))}
}
object SiteAccess{
 private var app:Context?=null
 fun init(context:Context){app=context.applicationContext}
 fun authenticated(url:String):String{val session=app?.let{AccountStore(it).session()}?:return url;return url+(if('?' in url)"&" else "?")+"auth="+URLEncoder.encode(session.token,"UTF-8")}
 suspend fun json(path:String):JSONObject=withContext(Dispatchers.IO){
  val connection=URL(authenticated("https://nnnsss-23f2f-default-rtdb.firebaseio.com/$path.json")).openConnection() as HttpURLConnection
  connection.connectTimeout=10000;connection.readTimeout=10000
  try{check(connection.responseCode in 200..299){"Não foi possível verificar sua conta. Tente novamente."};val raw=connection.inputStream.bufferedReader().use{it.readText()};if(raw.trim()=="null")JSONObject()else JSONObject(raw)}finally{connection.disconnect()}
 }
 suspend fun checkBan(context:Context):SiteBan{
  init(context);val store=AccountStore(context);val session=store.session()?:return SiteBan()
  val fresh=AccountRepository().refresh(session);store.save(fresh)
  val value=json("userBans/${fresh.uid}");context.getSharedPreferences("mp_bans",0).edit().putString(fresh.uid,value.toString()).apply()
  return SiteBan.parse(value)
 }
 fun cachedBan(context:Context,uid:String)=SiteBan.parse(JSONObject(context.getSharedPreferences("mp_bans",0).getString(uid,"{}")?:"{}"))
 suspend fun requireAllowed(context:Context){check(!checkBan(context).blocks()){ "Sua conta está suspensa. Consulte o motivo no aplicativo." }}
}
