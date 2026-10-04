package online.mpscan.app.data
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import online.mpscan.app.MainActivity
import java.util.concurrent.TimeUnit

class NewChapterWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){
 override suspend fun doWork():Result=try{NotificationSync.sync(applicationContext);Result.success()}catch(e:CancellationException){throw e}catch(_:Exception){Result.retry()}
 companion object {
  fun schedule(context:Context){val request=PeriodicWorkRequestBuilder<NewChapterWorker>(15,TimeUnit.MINUTES).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build();WorkManager.getInstance(context).enqueueUniquePeriodicWork("new-chapter-watch",ExistingPeriodicWorkPolicy.UPDATE,request)}
  fun runNow(context:Context){val request=OneTimeWorkRequestBuilder<NewChapterWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build();WorkManager.getInstance(context).enqueueUniqueWork("new-chapter-immediate",ExistingWorkPolicy.KEEP,request)}
 }
}
object NotificationSync {
 private val lock=Mutex()
 suspend fun sync(context:Context)=lock.withLock { withContext(Dispatchers.IO){
  if(!SettingsStore(context).notifications||!NotificationManagerCompat.from(context).areNotificationsEnabled())return@withContext
  if(android.os.Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return@withContext
  val store=AccountStore(context);val old=store.session()?:return@withContext
  if(SiteAccess.checkBan(context).blocks())return@withContext
  val session=store.session()?:old;val repo=LibraryRepository()
  val prefs=context.getSharedPreferences("mp_scan_remote_notifications_${session.uid}",Context.MODE_PRIVATE)
  val seen=prefs.getStringSet("shown",emptySet())!!.toMutableSet()
  val remote=repo.notifications(session)
  remote.keys().asSequence().mapNotNull{id->remote.optJSONObject(id)?.let{id to it}}.sortedBy{it.second.optLong("data")}.forEach{(id,n)->
   if(id !in seen){val title=n.optString("titulo",n.optString("title","Novidades da MP SCAN"));val text=n.optString("texto",n.optString("text"));if(post(context,title,text,"remote-$id")){seen+=id;prefs.edit().putStringSet("shown",seen).apply()}}
  }
  val preferences=SiteAccess.json("notificacoesPreferencias/${session.uid}")
  val chapters=CatalogRepository()
  val state=context.getSharedPreferences("mp_scan_chapter_watch_${session.uid}",Context.MODE_PRIVATE)
  NotificationRules.subscriptions(preferences).forEach{workId->
   val available=chapters.chapters(workId).filter{it.available};val known=state.getStringSet("known_$workId",null)
   val since=preferences.optJSONObject(workId)?.optLong("data",System.currentTimeMillis())?:System.currentTimeMillis()
   val new=NotificationRules.newChapters(available,known.orEmpty(),since)
   if(new.isNotEmpty()){
    val work=SiteAccess.json("obras/$workId");val title=work.optString("nome",work.optString("titulo","Nova atualização"))
    val text=if(new.size==1)"${new.first().label} já está disponível." else "${new.size} novos capítulos já estão disponíveis."
    if(!post(context,title,text,"chapter-$workId-${new.last().id}"))return@forEach
   }
   state.edit().putStringSet("known_$workId",available.map{it.id}.toSet()).apply()
  }
 }}
 private fun post(context:Context,title:String,text:String,id:String):Boolean {
  val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  if(android.os.Build.VERSION.SDK_INT>=26){manager.createNotificationChannel(NotificationChannel("new_chapters","Novos capítulos",NotificationManager.IMPORTANCE_DEFAULT));if(manager.getNotificationChannel("new_chapters")?.importance==NotificationManager.IMPORTANCE_NONE)return false}
  val pending=PendingIntent.getActivity(context,id.hashCode(),Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  return try{manager.notify(id.hashCode(),NotificationCompat.Builder(context,"new_chapters").setSmallIcon(online.mpscan.app.R.drawable.ic_notification).setContentTitle(title).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true).setContentIntent(pending).build());true}catch(_:SecurityException){false}
 }
}
