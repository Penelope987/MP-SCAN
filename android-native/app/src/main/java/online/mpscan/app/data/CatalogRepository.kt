package online.mpscan.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CatalogRepository(private val base:String="https://nnnsss-23f2f-default-rtdb.firebaseio.com"){
    suspend fun works():List<Work> = withContext(Dispatchers.IO){
        val c=URL("$base/obras.json").openConnection() as HttpURLConnection
        c.connectTimeout=15000;c.readTimeout=25000
        val text=c.inputStream.bufferedReader().use{it.readText()};c.disconnect()
        val root=JSONObject(text)
        root.keys().asSequence().mapNotNull{id->root.optJSONObject(id)?.let{it.toWork(id)}}.filter{it.title.isNotBlank()}.sortedByDescending{it.updatedAt}.toList()
    }
    suspend fun chapters(workId:String):List<Chapter> = withContext(Dispatchers.IO){
        val c=URL("$base/capitulos/$workId.json").openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=25000
        val text=c.inputStream.bufferedReader().use{it.readText()};c.disconnect();val root=JSONObject(text)
        root.keys().asSequence().mapNotNull{id->root.optJSONObject(id)?.let{o->Chapter(id,o.string("numero","number").replace(',','.').toDoubleOrNull(),o.string("titulo","title"),!o.has("publicado")||o.optBoolean("publicado",true),o.long("atualizadoEm","updatedAt"))}}.filter{it.published}.sortedByDescending{it.number?:-1.0}.toList()
    }
    private fun JSONObject.toWork(id:String)=Work(id,string("nome","name","titulo"),string("sinopse","synopsis"),string("capa","cover","coverURL"),string("banner","bannerURL"),string("tipo","type"),string("status"),string("autor","author"),strings(opt("generos")?:opt("genres")),long("atualizadoEm","updatedAt"),long("cliques","leituras","reads"))
    private fun JSONObject.string(vararg k:String)=k.firstNotNullOfOrNull{optString(it).trim().takeIf(String::isNotBlank)}?:""
    private fun JSONObject.long(vararg k:String)=k.firstNotNullOfOrNull{opt(it)?.toString()?.toLongOrNull()}?:0L
    private fun strings(v:Any?):List<String> = when(v){is JSONArray->(0 until v.length()).mapNotNull{v.optString(it).takeIf(String::isNotBlank)};is JSONObject->v.keys().asSequence().mapNotNull{v.optString(it).takeIf(String::isNotBlank)}.toList();else->v?.toString()?.split(',')?.map(String::trim)?.filter(String::isNotBlank)?:emptyList()}
}
