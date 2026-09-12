package online.mpscan.app.data

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class OfflineStore(context: Context) {
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

    suspend fun download(
        work: Work,
        chapter: Chapter,
        pageUrls: List<String>,
        onProgress: (Int) -> Unit
    ): List<String> = withContext(Dispatchers.IO) {
        require(pageUrls.isNotEmpty()) { "Capítulo sem páginas" }
        val destination = chapterFolder(work.id, chapter.id)
        val temporary = File(destination.parentFile, destination.name + "_download")
        temporary.deleteRecursively()
        check(temporary.mkdirs()) { "Não foi possível preparar o download" }
        try {
            val names = pageUrls.mapIndexed { index, source ->
                val extension = extensionFor(source)
                val name = "%04d.%s".format(index + 1, extension)
                File(temporary, name).writeBytes(readBytes(source))
                onProgress(((index + 1) * 100) / pageUrls.size)
                name
            }
            File(temporary, "chapter.json").writeText(
                JSONObject()
                    .put("workId", work.id)
                    .put("workTitle", work.title)
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

    private fun chapterFolder(workId: String, chapterId: String) =
        File(File(root, safe(workId)), safe(chapterId))

    private fun safe(value: String) = value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun readBytes(source: String): ByteArray {
        if (source.startsWith("data:", ignoreCase = true)) {
            val comma = source.indexOf(',')
            require(comma > 0) { "Imagem inválida" }
            return Base64.decode(source.substring(comma + 1), Base64.DEFAULT)
        }
        val connection = URL(source).openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "MP-SCAN-Android")
        return try {
            check(connection.responseCode in 200..299) { "Falha ao baixar página" }
            connection.inputStream.use { it.readBytes() }
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
