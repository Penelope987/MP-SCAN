package online.mpscan.app.data


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID


data class UserCollection(
    val id: String,
    val name: String,
    val description: String,
    val isPublic: Boolean,
    val cover: String,
    val workIds: Set<String>
)


class LibraryRepository(private val base: String = "https://nnnsss-23f2f-default-rtdb.firebaseio.com") {
    private fun e(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun url(path: String, session: AccountSession) = "$base/$path.json?auth=${e(session.token)}"


    suspend fun collections(session: AccountSession): List<UserCollection> = withContext(Dispatchers.IO) {
        val root = request(url("colecoes/${e(session.uid)}", session))
        root.keys().asSequence().mapNotNull { id -> root.optJSONObject(id)?.let { value ->
            val works = value.optJSONObject("obras") ?: value.optJSONObject("items") ?: JSONObject()
            UserCollection(id, value.optString("nome", value.optString("name", "Coleção")),
                value.optString("descricao"), value.optBoolean("publico", false), value.optString("capa"),
                works.keys().asSequence().toSet())
        }}.sortedBy { it.name.lowercase() }.toList()
    }


    suspend fun createCollection(session: AccountSession, name: String, isPublic: Boolean): UserCollection = withContext(Dispatchers.IO) {
        val clean = name.trim(); require(clean.isNotBlank()) { "Dê um nome para a coleção." }
        val id = "app_${UUID.randomUUID().toString().replace("-", "")}"
        val payload = JSONObject().put("nome", clean).put("descricao", "").put("publico", isPublic)
            .put("capa", "").put("obras", JSONObject()).put("data", System.currentTimeMillis()).put("atualizadoEm", System.currentTimeMillis())
        request(url("colecoes/${e(session.uid)}/$id", session), "PUT", payload.toString())
        if (isPublic) request(url("colecoesPublicas/${e(session.uid)}/$id", session), "PUT", payload.toString())
        UserCollection(id, clean, "", isPublic, "", emptySet())
    }


    suspend fun setWork(session: AccountSession, collection: UserCollection, workId: String, selected: Boolean) = withContext(Dispatchers.IO) {
        val privatePath = "colecoes/${e(session.uid)}/${e(collection.id)}/obras/${e(workId)}"
        val publicPath = "colecoesPublicas/${e(session.uid)}/${e(collection.id)}/obras/${e(workId)}"
        if (selected) {
            val entry = JSONObject().put("data", System.currentTimeMillis()).toString()
            request(url(privatePath, session), "PUT", entry)
            if (collection.isPublic) request(url(publicPath, session), "PUT", entry)
        } else {
            request(url(privatePath, session), "DELETE")
            if (collection.isPublic) runCatching { request(url(publicPath, session), "DELETE") }
        }
    }


    suspend fun notificationEnabled(session: AccountSession, workId: String): Boolean = withContext(Dispatchers.IO) {
        request(url("notificacoesPreferencias/${e(session.uid)}/${e(workId)}", session)).length() > 0
    }


    suspend fun notificationWorkIds(session: AccountSession): Set<String> = withContext(Dispatchers.IO) {
        val root = request(url("notificacoesPreferencias/${e(session.uid)}", session))
        root.keys().asSequence().toSet()
    }

    suspend fun setNotification(session: AccountSession, workId: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        val target = url("notificacoesPreferencias/${e(session.uid)}/${e(workId)}", session)
        if (enabled) request(target, "PUT", JSONObject().put("data", System.currentTimeMillis()).toString()) else request(target, "DELETE")
    }


    fun notifications(session: AccountSession): JSONObject = request(url("notificacoes/${e(session.uid)}", session))


    private fun request(target: String, method: String = "GET", body: String? = null): JSONObject {
        val connection = URL(target).openConnection() as HttpURLConnection
        connection.requestMethod = method; connection.connectTimeout = 15_000; connection.readTimeout = 30_000
        connection.setRequestProperty("Content-Type", "application/json")
        if (body != null) { connection.doOutput = true; connection.outputStream.use { it.write(body.toByteArray()) } }
        val ok = connection.responseCode in 200..299
        val text = (if (ok) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (!ok) error("Não foi possível sincronizar com sua conta.")
        return if (text.isBlank() || text == "null") JSONObject() else runCatching { JSONObject(text) }.getOrElse { JSONObject().put("value", text.trim('"')) }
    }
}

