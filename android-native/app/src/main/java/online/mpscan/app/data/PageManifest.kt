package online.mpscan.app.data

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/** Decode the same array/object page manifests used by the Blogger publisher. */
object PageManifest {
    fun parse(text: String): List<String> {
        val raw = JSONTokener(text).nextValue()
        fun source(value: Any?): String? = when (value) {
            is String -> value.trim().takeIf { it.startsWith("https://") || it.startsWith("http://") || it.startsWith("data:image/") }
            is JSONObject -> listOf("dataUrl", "url", "imagemUrl", "imagem", "src").firstNotNullOfOrNull { source(value.opt(it)) }
            else -> null
        }
        if(raw is String)return listOfNotNull(source(raw))
        if(raw is JSONObject)source(raw)?.let{return listOf(it)}
        fun order(key: String, value: Any?): Double = (value as? JSONObject)?.let {
            listOf("ordem", "order", "index", "posicao", "position", "pagina", "numero").firstNotNullOfOrNull { field -> it.opt(field)?.toString()?.toDoubleOrNull() }
        } ?: key.toDoubleOrNull() ?: Double.MAX_VALUE
        val entries = when (raw) {
            is JSONArray -> (0 until raw.length()).map { it.toString() to raw.opt(it) }
            is JSONObject -> raw.keys().asSequence().map { it to raw.opt(it) }.toList()
            else -> emptyList()
        }
        return entries.sortedWith(compareBy<Pair<String, Any?>> { order(it.first, it.second) }.thenBy { it.first })
            .mapNotNull { source(it.second) }
    }
}
