package online.mpscan.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import online.mpscan.app.MainActivity
import java.util.concurrent.TimeUnit

class NewChapterWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!SettingsStore(applicationContext).notifications) return Result.success()
        val accountStore = AccountStore(applicationContext)
        val oldSession = accountStore.session() ?: return Result.success()
        return runCatching {
            val account = AccountRepository()
            val session = account.refresh(oldSession).also(accountStore::save)
            val notifications = LibraryRepository().notifications(session)
            val seen = applicationContext.getSharedPreferences("mp_scan_remote_notifications", Context.MODE_PRIVATE)
                .getStringSet("shown", emptySet())?.toMutableSet() ?: mutableSetOf()
            notifications.keys().asSequence().mapNotNull { id -> notifications.optJSONObject(id)?.let { id to it } }
                .filter { (id, item) -> id !in seen && item.optString("tipo") == "chapter" }
                .sortedBy { it.second.optLong("data") }.forEach { (id, item) ->
                    notifyNewChapter(item.optString("titulo", "Novo capítulo"), item.optString("texto", "Um novo capítulo está disponível."), id)
                    seen += id
                }
            applicationContext.getSharedPreferences("mp_scan_remote_notifications", Context.MODE_PRIVATE)
                .edit().putStringSet("shown", seen.takeLast(200).toSet()).apply()
            Result.success()
        }.getOrElse { Result.retry() }
    }

    private fun notifyNewChapter(title: String, text: String, notificationId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Novos capítulos", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pending = PendingIntent.getActivity(applicationContext, notificationId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL).setSmallIcon(online.mpscan.app.R.mipmap.ic_launcher)
            .setContentTitle(title).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true).setContentIntent(pending).build()
        manager.notify(notificationId.hashCode(), notification)
    }

    companion object {
        private const val CHANNEL = "new_chapters"
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NewChapterWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("new-chapter-watch", ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
