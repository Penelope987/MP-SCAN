package online.mpscan.app.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints

class ChapterDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val workId = inputData.getString(WORK_ID) ?: return Result.failure()
        val chapterId = inputData.getString(CHAPTER_ID) ?: return Result.failure()
        val work = Work(
            id = workId,
            title = inputData.getString(WORK_TITLE).orEmpty(),
            synopsis = "",
            cover = inputData.getString(WORK_COVER).orEmpty(),
            banner = "", type = "", status = "", author = "", genres = emptyList(), updatedAt = 0, reads = 0
        )
        val chapter = Chapter(
            id = chapterId,
            number = inputData.getDouble(CHAPTER_NUMBER, Double.NaN).takeUnless(Double::isNaN),
            title = inputData.getString(CHAPTER_TITLE).orEmpty(), published = true, updatedAt = 0
        )
        return runCatching {
            val pages = CatalogRepository().pages(workId, chapterId)
            OfflineStore(applicationContext).download(work, chapter, pages) { value ->
                setProgressAsync(Data.Builder().putInt(PROGRESS, value).build())
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val WORK_ID = "workId"; const val WORK_TITLE = "workTitle"; const val WORK_COVER = "workCover"
        const val CHAPTER_ID = "chapterId"; const val CHAPTER_NUMBER = "chapterNumber"; const val CHAPTER_TITLE = "chapterTitle"
        const val PROGRESS = "progress"
        fun uniqueName(workId: String, chapterId: String) = "chapter-download-$workId-$chapterId"
        fun enqueue(context: Context, work: Work, chapter: Chapter) {
            val data = Data.Builder().putString(WORK_ID, work.id).putString(WORK_TITLE, work.title)
                .putString(WORK_COVER, work.cover).putString(CHAPTER_ID, chapter.id)
                .putDouble(CHAPTER_NUMBER, chapter.number ?: Double.NaN).putString(CHAPTER_TITLE, chapter.title).build()
            val request = OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(data).addTag("work-download-${work.id}").addTag(uniqueName(work.id, chapter.id)).build()
            WorkManager.getInstance(context).enqueueUniqueWork(uniqueName(work.id, chapter.id), ExistingWorkPolicy.KEEP, request)
        }
    }
}
