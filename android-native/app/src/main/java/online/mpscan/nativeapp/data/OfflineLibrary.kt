package online.mpscan.nativeapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import online.mpscan.nativeapp.model.Chapter
import online.mpscan.nativeapp.model.Work
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Device-owned cache. Catalog metadata and downloaded chapter images survive restarts. */
class OfflineLibrary(private val context: Context) {
    private val root = File(context.filesDir, "offline_library").apply { mkdirs() }

    suspend fun saveCatalog(works: List<Work>) = withContext(Dispatchers.IO) {
        atomicWrite(File(root, "catalog.json"), JSONArray().apply { works.forEach { put(it.json()) } }.toString())
    }

    suspend fun loadCatalog(): List<Work> = withContext(Dispatchers.IO) {
        val file = File(root, "catalog.json")
        if (!file.exists()) return@withContext emptyList()
        runCatching { JSONArray(file.readText()).objects().map { it.work() } }.getOrDefault(emptyList())
    }

    suspend fun saveChapters(workId: String, chapters: List<Chapter>) = withContext(Dispatchers.IO) {
        val json = JSONArray().apply { chapters.forEach { put(it.json()) } }
        atomicWrite(File(root, "chapters_${safe(workId)}.json"), json.toString())
    }

    suspend fun loadChapters(workId: String): List<Chapter> = withContext(Dispatchers.IO) {
        val file = File(root, "chapters_${safe(workId)}.json")
        if (!file.exists()) return@withContext emptyList()
        runCatching { JSONArray(file.readText()).objects().map { it.chapter() } }.getOrDefault(emptyList())
    }

    suspend fun downloadChapter(work: Work, chapter: Chapter, urls: List<String>, progress: (Int) -> Unit): List<String> = withContext(Dispatchers.IO) {
        require(urls.isNotEmpty()) { "O capítulo não possui páginas." }
        val folder = chapterFolder(work.id, chapter.id).apply { mkdirs() }
        val completed = mutableListOf<String>()
        urls.forEachIndexed { index, address ->
            val target = File(folder, "%04d.img".format(index + 1))
            if (!target.exists() || target.length() == 0L) download(address, target)
            completed += target.absolutePath
            progress(((index + 1) * 100) / urls.size)
        }
        val meta = JSONObject().put("work", work.json()).put("chapter", chapter.json()).put("pages", JSONArray(completed))
        atomicWrite(File(folder, "chapter.json"), meta.toString())
        completed
    }

    suspend fun localPages(workId: String, chapterId: String): List<String> = withContext(Dispatchers.IO) {
        val meta = File(chapterFolder(workId, chapterId), "chapter.json")
        if (!meta.exists()) return@withContext emptyList()
        runCatching { JSONObject(meta.readText()).getJSONArray("pages").strings().filter { File(it).exists() } }.getOrDefault(emptyList())
    }

    suspend fun downloads(): List<OfflineChapter> = withContext(Dispatchers.IO) {
        root.walkTopDown().filter { it.name == "chapter.json" }.mapNotNull { file ->
            runCatching { val o = JSONObject(file.readText()); OfflineChapter(o.getJSONObject("work").work(), o.getJSONObject("chapter").chapter(), o.getJSONArray("pages").length()) }.getOrNull()
        }.toList()
    }

    suspend fun remove(workId: String, chapterId: String) = withContext(Dispatchers.IO) { chapterFolder(workId, chapterId).deleteRecursively() }

    private fun chapterFolder(workId: String, chapterId: String) = File(root, "downloads/${safe(workId)}/${safe(chapterId)}")
    private fun safe(value: String) = value.replace(Regex("[^A-Za-z0-9._-]"), "_")
    private fun atomicWrite(file: File, text: String) { file.parentFile?.mkdirs(); val tmp = File(file.parentFile, file.name + ".tmp"); tmp.writeText(text); if (!tmp.renameTo(file)) { file.writeText(text); tmp.delete() } }
    private fun download(address: String, target: File) {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000; connection.readTimeout = 45_000; connection.instanceFollowRedirects = true
        try { connection.inputStream.use { input -> target.outputStream().use { input.copyTo(it) } } } catch (e: Exception) { target.delete(); throw e } finally { connection.disconnect() }
    }
}

data class OfflineChapter(val work: Work, val chapter: Chapter, val pageCount: Int)

private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
private fun JSONArray.strings() = (0 until length()).map { getString(it) }
private fun Work.json() = JSONObject().put("id", id).put("title", title).put("alternativeTitle", alternativeTitle).put("synopsis", synopsis).put("cover", cover).put("banner", banner).put("type", type).put("status", status).put("author", author).put("genres", JSONArray(genres)).put("sensitive", sensitive).put("adult", adult).put("published", published).put("updatedAt", updatedAt).put("reads", reads)
private fun JSONObject.work() = Work(getString("id"), optString("title"), optString("alternativeTitle"), optString("synopsis"), optString("cover"), optString("banner"), optString("type"), optString("status"), optString("author"), optJSONArray("genres")?.strings() ?: emptyList(), optBoolean("sensitive"), optBoolean("adult"), optBoolean("published", true), optLong("updatedAt"), optLong("reads"))
private fun Chapter.json() = JSONObject().put("id", id).put("workId", workId).put("number", number ?: JSONObject.NULL).put("title", title).put("published", published).put("updatedAt", updatedAt)
private fun JSONObject.chapter() = Chapter(getString("id"), getString("workId"), if (isNull("number")) null else getDouble("number"), optString("title"), optBoolean("published", true), optLong("updatedAt"))
