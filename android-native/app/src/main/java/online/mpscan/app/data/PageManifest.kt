package online.mpscan.app.data

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/** Decode the same array/object page manifests used by the Blogger publisher. */
object PageManifest {
    fun normalize(value:String):String? {
        val clean=value.trim()
        return when {
            clean.startsWith("data:image/",true)->clean
            clean.startsWith("https://")->clean.replace("&amp;","&").replace(" ","%20")
            clean.startsWith("http://")->"https://"+clean.removePrefix("http://").replace("&amp;","&").replace(" ","%20")
            clean.startsWith("//")->"https:"+clean.replace("&amp;","&").replace(" ","%20")
            clean.startsWith("/9j/")->"data:image/jpeg;base64,$clean"
            clean.startsWith("iVBORw0KGgo")->"data:image/png;base64,$clean"
            clean.startsWith("R0lGOD")->"data:image/gif;base64,$clean"
            clean.startsWith("UklGR")->"data:image/webp;base64,$clean"
            else->null
        }
    }
    fun lazyReferences(root:JSONObject,path:String,version:Long?=null):List<String>? {
        val keys=root.keys().asSequence().toList()
        if(keys.isEmpty()||keys.any{!Regex("(?:pagina_)?[0-9]+").matches(it)||root.opt(it)!=true})return null
        return keys.sortedBy{it.removePrefix("pagina_").toLongOrNull()?:Long.MAX_VALUE}.map{"mpscan-page:$path/$it"+(if(version==null)""else"?v=$version")}
    }
    fun parse(text: String): List<String> {
        val raw = JSONTokener(text).nextValue()
        fun source(value: Any?): String? = when (value) {
            is String -> normalize(value)
            is JSONObject -> listOf("dataUrl", "url", "imagemUrl", "imagem", "src", "page", "base64", "data", "imageUrl", "imageURL").firstNotNullOfOrNull { source(value.opt(it)) }
            else -> null
        }
        if(raw is String)return listOfNotNull(source(raw))
        if(raw is JSONObject){source(raw)?.let{return listOf(it)};listOf("paginas","pages","imagens","images","data").forEach{field->val nested=raw.opt(field);if(nested is JSONArray||nested is JSONObject)parse(nested.toString()).takeIf{it.isNotEmpty()}?.let{return it}}}
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
