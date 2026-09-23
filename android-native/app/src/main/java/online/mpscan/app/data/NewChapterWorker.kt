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
        val store = WorkSubscriptionsStore(applicationContext)
        val subscribed = store.ids()
        if (subscribed.isEmpty()) return Result.success()
        return runCatching {
            val works = CatalogRepository().works().associateBy { it.id }
            subscribed.forEach { workId ->
                val chapters = CatalogRepository().chapters(workId)
                val newest = chapters.maxOfOrNull { maxOf(it.updatedAt, it.createdAt, ((it.number ?: 0.0) * 1000).toLong()) } ?: 0L
                val previous = store.lastSeen(workId)
                if (previous > 0 && newest > previous) notifyNewChapter(works[workId]?.title ?: "MP SCAN", workId)
                if (newest > 0) store.setLastSeen(workId, newest)
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }

    private fun notifyNewChapter(title: String, workId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Novos capítulos", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pending = PendingIntent.getActivity(applicationContext, workId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL).setSmallIcon(online.mpscan.app.R.mipmap.ic_launcher)
            .setContentTitle("Novo capítulo disponível").setContentText(title).setAutoCancel(true).setContentIntent(pending).build()
        manager.notify(workId.hashCode(), notification)
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
