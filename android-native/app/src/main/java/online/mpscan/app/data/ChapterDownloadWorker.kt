package online.mpscan.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.AtomicFile
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class ChapterDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val workId = inputData.getString(WORK_ID) ?: return Result.failure()
        try {
            setForeground(foregroundInfo())
            val metadata = withContext(Dispatchers.IO) {
                synchronized(metadataLock) {
                    val file = metadataFile(applicationContext, workId)
                    if (file.baseFile.exists()) JSONObject(file.openRead().bufferedReader().use { it.readText() })
                    else JSONObject()
                }
            }
            val work = Work(
                id = workId, title = metadata.optString(WORK_TITLE, inputData.getString(WORK_TITLE).orEmpty()),
                synopsis = "", cover = metadata.optString(WORK_COVER, inputData.getString(WORK_COVER).orEmpty()),
                banner = "", type = "", status = "", author = "", genres = emptyList(), updatedAt = 0, reads = 0
            )
            val repository = CatalogRepository()
            val all = inputData.getBoolean(DOWNLOAD_ALL, false)
            val chapters = if (all) repository.chapters(workId).filter { it.published } else listOf(
                Chapter(
                    id = inputData.getString(CHAPTER_ID) ?: return Result.failure(),
                    number = inputData.getDouble(CHAPTER_NUMBER, Double.NaN).takeUnless(Double::isNaN),
                    title = inputData.getString(CHAPTER_TITLE).orEmpty(), published = true, updatedAt = 0
                )
            )
            check(chapters.isNotEmpty()) { "Nenhum capítulo disponível para baixar." }
            val store = OfflineStore(applicationContext)
            chapters.forEachIndexed { index, chapter ->
                if (withContext(Dispatchers.IO) { store.localPages(workId, chapter.id).isEmpty() }) {
                    store.download(work, chapter, repository.pages(workId, chapter.id)) { value ->
                        setProgressAsync(Data.Builder().putInt(PROGRESS, (index * 100 + value) / chapters.size)
                            .putString(CHAPTER_ID, chapter.id).putInt(CHAPTER_PROGRESS, value).build())
                    }
                }
                setProgress(Data.Builder().putInt(PROGRESS, (index + 1) * 100 / chapters.size).build())
            }
            return Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (error is IOException && runAttemptCount < 3) return Result.retry()
            return Result.failure(Data.Builder().putString(ERROR,
                (error.message ?: "Não foi possível concluir o download. Tente novamente.").take(500)).build())
        }
    }

    private fun foregroundInfo(): ForegroundInfo {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Downloads de capítulos", NotificationManager.IMPORTANCE_LOW)
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("MP SCAN • Baixando capítulos")
            .setContentText("O download continua enquanto você usa outros aplicativos.")
            .setOngoing(true).setOnlyAlertOnce(true).setProgress(0, 0, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancelar",
                WorkManager.getInstance(applicationContext).createCancelPendingIntent(id))
            .build()
        return if (Build.VERSION.SDK_INT >= 29)
            ForegroundInfo(id.hashCode(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(id.hashCode(), notification)
    }

    companion object {
        const val WORK_ID = "workId"; const val WORK_TITLE = "workTitle"; const val WORK_COVER = "workCover"
        const val CHAPTER_ID = "chapterId"; const val CHAPTER_NUMBER = "chapterNumber"; const val CHAPTER_TITLE = "chapterTitle"
        const val PROGRESS = "progress"; const val CHAPTER_PROGRESS = "chapterProgress"; const val ERROR = "error"
        const val DOWNLOAD_ALL = "downloadAll"
        private const val CHANNEL = "chapter-downloads"
        private val metadataLock = Any()
        fun uniqueName(workId: String, chapterId: String) = "chapter-download-$workId-$chapterId"

        private fun metadataFile(context: Context, workId: String): AtomicFile {
            val key = MessageDigest.getInstance("SHA-256").digest(workId.toByteArray())
                .joinToString("") { "%02x".format(it) }
            return AtomicFile(File(File(context.filesDir, "download_metadata"), "$key.json"))
        }

        private suspend fun saveMetadata(context: Context, work: Work) = withContext(Dispatchers.IO) {
            synchronized(metadataLock) {
                val file = metadataFile(context, work.id)
                file.baseFile.parentFile?.mkdirs()
                val stream = file.startWrite()
                try {
                    stream.write(JSONObject().put(WORK_TITLE, work.title).put(WORK_COVER, work.cover).toString().toByteArray())
                    file.finishWrite(stream)
                } catch (error: Exception) {
                    file.failWrite(stream)
                    throw error
                }
            }
        }

        suspend fun enqueue(context: Context, work: Work, chapter: Chapter) {
            saveMetadata(context, work)
            val data = requestData(work, chapter)
            submit(context, work.id, uniqueName(work.id, chapter.id), data)
        }

        suspend fun enqueueAll(context: Context, work: Work) {
            saveMetadata(context, work)
            submit(context, work.id, "work-download-all-${work.id}",
                requestData(work, null))
        }

        internal fun requestData(work: Work, chapter: Chapter?): Data {
            val data = Data.Builder().putString(WORK_ID, work.id)
            if (chapter == null) data.putBoolean(DOWNLOAD_ALL, true)
            else data.putString(CHAPTER_ID, chapter.id)
                .putDouble(CHAPTER_NUMBER, chapter.number ?: Double.NaN)
                .putString(CHAPTER_TITLE, chapter.title.take(300))
            return data.build()
        }

        private suspend fun submit(context: Context, workId: String, name: String, data: Data) = withContext(Dispatchers.IO) {
            val request = OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(data).addTag("work-download-$workId").addTag(name).build()
            WorkManager.getInstance(context).enqueueUniqueWork(name, ExistingWorkPolicy.KEEP, request).result.get()
            Unit
        }
    }
}
