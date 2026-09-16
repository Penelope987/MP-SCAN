package online.mpscan.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CatalogRepository(private val base:String="https://nnnsss-23f2f-default-rtdb.firebaseio.com"){
    suspend fun newBadge():NewBadgeStyle = withContext(Dispatchers.IO){
        val c=URL("$base/config/newBadge.json").openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=25000
        val text=c.inputStream.bufferedReader().use{it.readText()};c.disconnect();if(text.isBlank()||text=="null")return@withContext NewBadgeStyle()
        val x=JSONObject(text);NewBadgeStyle(
            x.optBoolean("enabled",true),x.optString("text","NOVO").take(18).ifBlank{"NOVO"},x.optString("imageUrl"),x.optString("backgroundMode","both"),
            x.optString("bgColor","#ff3f79"),x.optString("bgColor2","#8d2bff"),x.optString("textColor","#ffffff"),x.optString("glowColor","#ff4fa3"),
            x.optString("effect","pulse"),x.optString("textPosition","center"),x.optInt("fontSize",100).coerceIn(70,160),x.optInt("fontWeight",900).coerceIn(400,1000),
            x.optInt("letterSpacing",7).coerceIn(0,18),x.optInt("radius",999).coerceIn(4,999),x.optInt("size",100).coerceIn(80,145)
        )
    }
    suspend fun works():List<Work> = withContext(Dispatchers.IO){
        val c=URL("$base/obras.json").openConnection() as HttpURLConnection
        c.connectTimeout=15000;c.readTimeout=25000
        val text=c.inputStream.bufferedReader().use{it.readText()};c.disconnect()
        val root=JSONObject(text)
        root.keys().asSequence().mapNotNull{id->root.optJSONObject(id)?.let{it.toWork(id)}}.filter{it.title.isNotBlank()}.sortedByDescending{it.updatedAt}.toList()
    }
    suspend fun recentUpdates(works:List<Work>,limit:Int=4):List<RecentUpdate>{
        val candidates=works.distinctBy{it.id}.sortedByDescending{it.updatedAt}.take(12)
        return candidates.mapNotNull{work->runCatching{chapters(work.id).maxByOrNull{it.updatedAt.takeIf{time->time>0}?:((it.number?:0.0)*1000).toLong()}?.let{RecentUpdate(work,it,maxOf(work.updatedAt,it.updatedAt))}}.getOrNull()}.sortedByDescending{it.updatedAt}.take(limit)
    }
    suspend fun chapters(workId:String):List<Chapter> = withContext(Dispatchers.IO){
        val c=URL("$base/capitulos/$workId.json").openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=25000
        val text=c.inputStream.bufferedReader().use{it.readText()};c.disconnect();val root=JSONObject(text)
        root.keys().asSequence().mapNotNull{id->root.optJSONObject(id)?.let{o->Chapter(id,o.string("numero","number").replace(',','.').toDoubleOrNull(),o.string("titulo","title"),!o.has("publicado")||o.optBoolean("publicado",true),o.long("atualizadoEm","updatedAt"),o.long("criadoEm","createdAt"))}}.filter{it.published}.sortedByDescending{it.number?:-1.0}.toList()
    }
    suspend fun pages(workId:String,chapterId:String):List<String> = withContext(Dispatchers.IO){
        val c=URL("$base/capitulosPaginas/$workId/$chapterId.json").openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=30000
        val text=c.inputStream.bufferedReader().use{it.readText()};c.disconnect()
        val raw=if(text.trim().startsWith("["))JSONArray(text)else JSONObject(text)
        when(raw){is JSONArray->(0 until raw.length()).mapNotNull{raw.optString(it).takeIf(String::isNotBlank)};is JSONObject->raw.keys().asSequence().mapNotNull{k->val v=raw.opt(k);when(v){is String->v;is JSONObject->v.string("dataUrl","url","imagemUrl","imagem","src");else->null}.takeIf{!it.isNullOrBlank()}}.toList();else->emptyList()}
    }
    private fun JSONObject.toWork(id:String)=Work(id,string("nome","name","titulo"),string("sinopse","synopsis"),string("capa","cover","coverURL"),string("banner","bannerURL"),string("tipo","type"),string("status"),string("autor","author"),strings(opt("generos")?:opt("genres")),long("atualizadoEm","updatedAt"),long("cliques","leituras","reads"),string("subtitulo","nomeAlternativo","tituloAlternativo","alternateTitle","altName"),string("artista","artist"),string("ano","year"),string("scan","scanName"),string("hospedagem","hosting"),string("idioma","language").ifBlank{"Português"},schedule(optJSONObject("agendaAtualizacao")?:optJSONObject("updateSchedule")))
    private fun schedule(value:JSONObject?):String{val s=value?:return "Sem dia fixo";return when(s.string("tipo","type")){"weekly"->{val raw=s.opt("dias");val days=when(raw){is JSONArray->(0 until raw.length()).map{raw.optInt(it)};is JSONObject->raw.keys().asSequence().filter{raw.optBoolean(it)}.mapNotNull{it.toIntOrNull()}.toList();else->emptyList()}.distinct().sorted();val names=listOf("domingo","segunda-feira","terça-feira","quarta-feira","quinta-feira","sexta-feira","sábado");when(days.size){0->"Sem dia fixo";1->if(days[0] in 0..6)"Toda ${names[days[0]]}" else "Sem dia fixo";else->"Atualiza "+days.mapNotNull{names.getOrNull(it)}.joinToString(" e ")}};"monthly"->"Todo dia ${s.optInt("diaMes",s.optInt("dayOfMonth",1))} de cada mês";else->"Sem dia fixo"}}
    private fun JSONObject.string(vararg k:String)=k.firstNotNullOfOrNull{optString(it).trim().takeIf(String::isNotBlank)}?:""
    private fun JSONObject.long(vararg k:String)=k.firstNotNullOfOrNull{opt(it)?.toString()?.toLongOrNull()}?:0L
    private fun strings(v:Any?):List<String> = when(v){is JSONArray->(0 until v.length()).mapNotNull{v.optString(it).takeIf(String::isNotBlank)};is JSONObject->v.keys().asSequence().mapNotNull{v.optString(it).takeIf(String::isNotBlank)}.toList();else->v?.toString()?.split(',')?.map(String::trim)?.filter(String::isNotBlank)?:emptyList()}
}
