package online.mpscan.nativeapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppSession(val uid: String, val email: String, val idToken: String, val refreshToken: String = "")

data class AccountProfile(
    val name: String,
    val username: String,
    val ageVerified: Boolean,
    val ageStatus: String,
    val birthDateTimestamp: Long,
    val sensitiveAllowed: Boolean,
    val minorSensitiveApproved: Boolean
)

class FirebaseAuthRepository(
    private val apiKey: String = "AIzaSyAbpqQIxWuEnFolv3lNjNDoPKTGm0mtrxU"
) {
    suspend fun signIn(email: String, password: String): AppSession = authenticate("signInWithPassword", email, password)
    suspend fun createAccount(email: String, password: String): AppSession = authenticate("signUp", email, password)

    private suspend fun authenticate(action: String, email: String, password: String): AppSession = withContext(Dispatchers.IO) {
        val connection = URL("https://identitytoolkit.googleapis.com/v1/accounts:$action?key=$apiKey").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        val body = JSONObject().put("email", email.trim()).put("password", password).put("returnSecureToken", true).toString()
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        val json = runCatching { JSONObject(response) }.getOrDefault(JSONObject())
        if (!json.has("idToken")) throw IllegalStateException(authMessage(json.optJSONObject("error")?.optString("message")))
        AppSession(json.getString("localId"), json.optString("email", email.trim()), json.getString("idToken"), json.optString("refreshToken"))
    }

    suspend fun loadProfile(session: AppSession): AccountProfile = withContext(Dispatchers.IO) {
        val connection = URL("https://nnnsss-23f2f-default-rtdb.firebaseio.com/usuarios/${session.uid}.json?auth=${session.idToken}").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        val code = connection.responseCode
        connection.disconnect()
        if (code !in 200..299) throw IllegalStateException(if (code == 401) "Sua sessão expirou. Entre novamente." else "Não foi possível carregar os dados da conta.")
        val p = runCatching { JSONObject(response) }.getOrDefault(JSONObject())
        AccountProfile(
            name = p.optString("nome"),
            username = p.optString("nomeUsuario"),
            ageVerified = p.optBoolean("ageVerified", false),
            ageStatus = p.optString("ageVerificationStatus"),
            birthDateTimestamp = p.optLong("dataNascimentoTs", 0L),
            sensitiveAllowed = p.optBoolean("sensivelPermitido", false),
            minorSensitiveApproved = p.optBoolean("minorSensitiveApproved", false)
        )
    }

    fun save(context: Context, session: AppSession) {
        context.getSharedPreferences("mp_scan_session", Context.MODE_PRIVATE).edit()
            .putString("uid", session.uid).putString("email", session.email).putString("id_token", session.idToken)
            .putString("refresh_token", session.refreshToken).apply()
    }

    fun current(context: Context): AppSession? {
        val p = context.getSharedPreferences("mp_scan_session", Context.MODE_PRIVATE)
        val uid = p.getString("uid", null) ?: return null
        return AppSession(uid, p.getString("email", "").orEmpty(), p.getString("id_token", "").orEmpty(), p.getString("refresh_token", "").orEmpty())
    }

    fun signOut(context: Context) { context.getSharedPreferences("mp_scan_session", Context.MODE_PRIVATE).edit().clear().apply() }

    private fun authMessage(code: String?) = when (code?.substringBefore(" :")) {
        "EMAIL_EXISTS" -> "Este e-mail já possui uma conta."
        "INVALID_LOGIN_CREDENTIALS", "EMAIL_NOT_FOUND", "INVALID_PASSWORD" -> "E-mail ou senha incorretos."
        "WEAK_PASSWORD" -> "Use uma senha com pelo menos 6 caracteres."
        "INVALID_EMAIL" -> "Digite um e-mail válido."
        "USER_DISABLED" -> "Esta conta está desativada."
        else -> "Não foi possível acessar a conta agora."
    }
}
