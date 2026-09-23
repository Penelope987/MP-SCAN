package online.mpscan.app.data

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

data class OfflineChapter(
    val workId: String,
    val workTitle: String,
    val workCover: String,
    val chapterId: String,
    val chapterLabel: String,
    val pageCount: Int
)

class OfflineStore(context: Context) {
    companion object { private val downloadLock = Mutex() }
    private val root = File(context.filesDir, "mp_scan_downloads")

    fun localPages(workId: String, chapterId: String): List<String> {
        val folder = chapterFolder(workId, chapterId)
        val metadata = File(folder, "chapter.json")
        if (!metadata.isFile) return emptyList()
        return runCatching {
            val files = JSONObject(metadata.readText()).optJSONArray("files") ?: JSONArray()
            (0 until files.length()).mapNotNull { index ->
                File(folder, files.optString(index)).takeIf(File::isFile)?.toURI()?.toString()
            }.takeIf { it.size == files.length() } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun downloads(): List<OfflineChapter> = root.listFiles()
        ?.filter(File::isDirectory)
        ?.flatMap { workFolder ->
            workFolder.listFiles()?.filter { it.isDirectory && !it.name.endsWith("_download") }?.mapNotNull { chapterFolder ->
                runCatching {
                    val metadata = JSONObject(File(chapterFolder, "chapter.json").readText())
                    val files = metadata.optJSONArray("files") ?: JSONArray()
                    if (files.length() == 0 || (0 until files.length()).any { !File(chapterFolder, files.getString(it)).isFile }) return@runCatching null
                    OfflineChapter(
                        workId = metadata.getString("workId"),
                        workTitle = metadata.optString("workTitle", "Obra baixada"),
                        workCover = metadata.optString("workCover"),
                        chapterId = metadata.getString("chapterId"),
                        chapterLabel = metadata.optString("chapterLabel", "Capítulo"),
                        pageCount = files.length()
                    )
                }.getOrNull()
            } ?: emptyList()
        }
        ?.sortedWith(compareBy<OfflineChapter> { it.workTitle.lowercase() }.thenBy { it.chapterLabel })
        ?: emptyList()

    fun delete(workId: String, chapterId: String) {
        chapterFolder(workId, chapterId).deleteRecursively()
        File(root, safe(workId)).takeIf { it.listFiles().isNullOrEmpty() }?.delete()
    }

    suspend fun download(
        work: Work,
        chapter: Chapter,
        pageUrls: List<String>,
        onProgress: (Int) -> Unit
    ): List<String> = withContext(Dispatchers.IO) { downloadLock.withLock {
        val saved = localPages(work.id, chapter.id)
        if (saved.isNotEmpty()) return@withLock saved
        require(pageUrls.isNotEmpty()) { "Capítulo sem páginas" }
        val destination = chapterFolder(work.id, chapter.id)
        val temporary = File(destination.parentFile, destination.name + "_download")
        temporary.deleteRecursively()
        check(temporary.mkdirs()) { "Não foi possível preparar o download" }
        try {
            val completed = AtomicInteger(0)
            val limiter = Semaphore(4)
            val names = coroutineScope {
                pageUrls.mapIndexed { index, source ->
                    async(Dispatchers.IO) {
                        limiter.withPermit {
                            val extension = extensionFor(source)
                            val name = "%04d.%s".format(index + 1, extension)
                            writePage(source, File(temporary, name))
                            onProgress((completed.incrementAndGet() * 100) / pageUrls.size)
                            name
                        }
                    }
                }.awaitAll()
            }
            currentCoroutineContext().ensureActive()
            File(temporary, "chapter.json").writeText(
                JSONObject()
                    .put("workId", work.id)
                    .put("workTitle", work.title)
                    .put("workCover", work.cover)
                    .put("chapterId", chapter.id)
                    .put("chapterLabel", chapter.label)
                    .put("files", JSONArray(names))
                    .toString()
            )
            destination.deleteRecursively()
            check(temporary.renameTo(destination)) { "Não foi possível finalizar o download" }
            localPages(work.id, chapter.id)
        } catch (error: Throwable) {
            temporary.deleteRecursively()
            throw error
        }
    }

    }

    private fun chapterFolder(workId: String, chapterId: String) =
        File(File(root, safe(workId)), safe(chapterId))

    private fun safe(value: String) = value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun writePage(source: String, target: File) {
        if (source.startsWith("data:", ignoreCase = true)) {
            val comma = source.indexOf(',')
            require(comma > 0) { "Imagem inválida" }
            target.outputStream().use { output ->
                android.util.Base64InputStream(source.substring(comma + 1).byteInputStream(), Base64.DEFAULT).use { it.copyTo(output) }
            }
            check(target.length() > 0) { "Imagem vazia" }
            return
        }
        val connection = URL(source).openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "MP-SCAN-Android")
        try {
            if (connection.responseCode !in 200..299) throw java.io.IOException("Falha ao baixar página (HTTP ${connection.responseCode})")
            target.outputStream().use { output -> connection.inputStream.use { it.copyTo(output) } }
            check(target.length() > 0) { "Imagem vazia" }
        } finally {
            connection.disconnect()
        }
    }

    private fun extensionFor(source: String): String {
        val value = source.substringBefore(';').substringBefore('?').lowercase()
        return when {
            "png" in value -> "png"
            "webp" in value -> "webp"
            "gif" in value -> "gif"
            else -> "jpg"
        }
    }
}
