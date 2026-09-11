package online.mpscan.nativeapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

data class WorkComment(
    val id: String,
    val uid: String,
    val author: String,
    val username: String,
    val text: String,
    val timestamp: Long,
    val spoiler: Boolean,
    val reactions: Map<String, Int>,
    val myReaction: String
)

class CommunityRepository(
    private val databaseUrl: String = "https://nnnsss-23f2f-default-rtdb.firebaseio.com"
) {
    suspend fun comments(workId: String, currentUid: String? = null): List<WorkComment> = withContext(Dispatchers.IO) {
        val root = get("comentariosV1/obra/$workId")
        val identities = get("identidadesComentarios")
        root.keys().asSequence().mapNotNull { id ->
            val item = root.optJSONObject(id) ?: return@mapNotNull null
            val uid = item.optString("uid")
            val person = identities.optJSONObject(uid) ?: JSONObject()
            val rawReactions = item.optJSONObject("reacoes") ?: JSONObject()
            val reactionCounts = mutableMapOf<String, Int>()
            var own = ""
            rawReactions.keys().forEach { reactionUid ->
                val emoji = rawReactions.optJSONObject(reactionUid)?.optString("tipo").orEmpty()
                if (emoji.isNotBlank()) reactionCounts[emoji] = (reactionCounts[emoji] ?: 0) + 1
                if (reactionUid == currentUid) own = emoji
            }
            WorkComment(
                id = id,
                uid = uid,
                author = person.optString("nome", "Leitor MP SCAN"),
                username = person.optString("nomeUsuario"),
                text = item.optString("texto"),
                timestamp = item.optLong("data"),
                spoiler = item.optBoolean("spoiler", false),
                reactions = reactionCounts,
                myReaction = own
            )
        }.sortedByDescending { it.timestamp }.toList()
    }

    suspend fun react(workId: String, commentId: String, session: AppSession, emoji: String, remove: Boolean) = withContext(Dispatchers.IO) {
        val auth = URLEncoder.encode(session.idToken, Charsets.UTF_8.name())
        val path = "comentariosV1/obra/$workId/$commentId/reacoes/${session.uid}"
        val connection = URL("$databaseUrl/$path.json?auth=$auth").openConnection() as HttpURLConnection
        connection.requestMethod = if (remove) "DELETE" else "PUT"
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        if (!remove) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            val body = JSONObject().put("tipo", emoji).put("data", System.currentTimeMillis()).toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        val code = connection.responseCode
        connection.disconnect()
        if (code !in 200..299) throw IllegalStateException("Não foi possível registrar a reação.")
    }

    suspend fun post(workId: String, session: AppSession, text: String, spoiler: Boolean) = withContext(Dispatchers.IO) {
        val auth = URLEncoder.encode(session.idToken, Charsets.UTF_8.name())
        val connection = URL("$databaseUrl/comentariosV1/obra/$workId.json?auth=$auth").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        val body = JSONObject().put("uid", session.uid).put("data", System.currentTimeMillis()).put("texto", text.trim()).put("imagemUrl", "").put("spoiler", spoiler).toString()
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val response = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) throw IllegalStateException(if (code == 401 || code == 403) "Sua conta não tem permissão para comentar." else "Não foi possível publicar o comentário.")
        response
    }

    private fun get(path: String): JSONObject {
        val connection = URL("$databaseUrl/$path.json").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        val code = connection.responseCode
        val response = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) throw IllegalStateException("Não foi possível carregar os comentários.")
        return runCatching { JSONObject(response) }.getOrDefault(JSONObject())
    }
}
