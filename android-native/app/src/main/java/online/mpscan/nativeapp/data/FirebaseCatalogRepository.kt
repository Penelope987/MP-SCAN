package online.mpscan.nativeapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import online.mpscan.nativeapp.model.Chapter
import online.mpscan.nativeapp.model.Work
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class FirebaseCatalogRepository(
    private val databaseUrl: String = "https://nnnsss-23f2f-default-rtdb.firebaseio.com"
) {
    suspend fun loadWorks(): List<Work> = withContext(Dispatchers.IO) {
        val root = getObject("obras")
        root.keys().asSequence().mapNotNull { id -> root.optJSONObject(id)?.toWork(id) }
            .filter { it.published && !it.adult }
            .sortedByDescending { it.updatedAt }
            .toList()
    }

    suspend fun loadChapters(workId: String): List<Chapter> = withContext(Dispatchers.IO) {
        val root = getObject("capitulos/$workId")
        root.keys().asSequence().mapNotNull { id -> root.optJSONObject(id)?.toChapter(workId, id) }
            .filter { it.published }
            .sortedWith(compareByDescending<Chapter> { it.number }.thenByDescending { it.updatedAt })
            .toList()
    }

    suspend fun loadPages(workId: String, chapterId: String): List<String> = withContext(Dispatchers.IO) {
        val value = getValue("capitulosPaginas/$workId/$chapterId")
        when (value) {
            is JSONArray -> (0 until value.length()).mapNotNull { value.optString(it).takeIf(String::isNotBlank) }
            is JSONObject -> value.keys().asSequence().mapNotNull { key ->
                val item = value.opt(key)
                when (item) {
                    is String -> item
                    is JSONObject -> first(item, "url", "src", "dataUrl", "imagem")
                    else -> null
                }?.takeIf(String::isNotBlank)
            }.toList()
            else -> emptyList()
        }
    }

    private fun getObject(path: String): JSONObject = getValue(path) as? JSONObject ?: JSONObject()

    private fun getValue(path: String): Any {
        val connection = URL("$databaseUrl/$path.json").openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 25_000
        connection.requestMethod = "GET"
        return connection.inputStream.bufferedReader().use { reader ->
            val text = reader.readText()
            if (text.startsWith("{")) JSONObject(text) else if (text.startsWith("[")) JSONArray(text) else JSONObject.NULL
        }.also { connection.disconnect() }
    }

    private fun JSONObject.toWork(id: String) = Work(
        id = id,
        title = first(this, "nome", "name", "titulo") ?: "Sem nome",
        alternativeTitle = first(this, "nomeAlternativo", "altName") ?: "",
        synopsis = first(this, "sinopse", "synopsis") ?: "",
        cover = first(this, "capa", "cover", "coverURL") ?: "",
        banner = first(this, "banner", "bannerURL") ?: "",
        type = first(this, "tipo", "type") ?: "",
        status = first(this, "status") ?: "",
        author = first(this, "autor", "author") ?: "",
        genres = strings(opt("generos") ?: opt("genres")),
        sensitive = truth(opt("sensivel") ?: opt("sensitive")),
        adult = truth(opt("maior18") ?: opt("adult")),
        published = !hasFalse("publicado", "published"),
        updatedAt = longValue("atualizadoEm", "updatedAt"),
        reads = longValue("cliques", "leituras", "reads")
    )

    private fun JSONObject.toChapter(workId: String, id: String) = Chapter(
        id = id,
        workId = workId,
        number = first(this, "numero", "number")?.replace(',', '.')?.toDoubleOrNull(),
        title = first(this, "titulo", "title") ?: "",
        published = !hasFalse("publicado", "published"),
        updatedAt = longValue("atualizadoEm", "updatedAt")
    )

    private fun first(o: JSONObject, vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
        o.opt(key)?.toString()?.trim()?.takeUnless { it.isEmpty() || it == "null" }
    }
    private fun JSONObject.longValue(vararg keys: String) = keys.firstNotNullOfOrNull { opt(it)?.toString()?.toLongOrNull() } ?: 0L
    private fun JSONObject.hasFalse(vararg keys: String) = keys.any { has(it) && !truth(opt(it)) }
    private fun truth(v: Any?) = when (v) { is Boolean -> v; is Number -> v.toInt() != 0; else -> v?.toString()?.lowercase() in setOf("true", "1", "sim", "yes") }
    private fun strings(v: Any?): List<String> = when (v) {
        is JSONArray -> (0 until v.length()).mapNotNull { v.optString(it).takeIf(String::isNotBlank) }
        is JSONObject -> v.keys().asSequence().mapNotNull { v.optString(it).takeIf(String::isNotBlank) }.toList()
        else -> v?.toString()?.split(',')?.map(String::trim)?.filter(String::isNotBlank) ?: emptyList()
    }
}
