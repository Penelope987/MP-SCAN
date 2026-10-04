package online.mpscan.app.data

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CatalogRepository(private val base:String="https://nnnsss-23f2f-default-rtdb.firebaseio.com"){
    suspend fun newBadge():NewBadgeStyle = withContext(Dispatchers.IO){
        val c=URL(SiteAccess.authenticated("$base/config/newBadge.json")).openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=25000
        val text=c.inputStream.bufferedReader().use{it.readText()};c.disconnect();if(text.isBlank()||text=="null")return@withContext NewBadgeStyle()
        val x=JSONObject(text);NewBadgeStyle(
            x.optBoolean("enabled",true),x.optString("text","NOVO").take(18).ifBlank{"NOVO"},x.optString("imageUrl"),x.optString("backgroundMode","both"),
            x.optString("bgColor","#ff3f79"),x.optString("bgColor2","#8d2bff"),x.optString("textColor","#ffffff"),x.optString("glowColor","#ff4fa3"),
            x.optString("effect","pulse"),x.optString("textPosition","center"),x.optInt("fontSize",100).coerceIn(70,160),x.optInt("fontWeight",900).coerceIn(400,1000),
            x.optInt("letterSpacing",7).coerceIn(0,18),x.optInt("radius",999).coerceIn(4,999),x.optInt("size",100).coerceIn(80,145)
        )
    }
    suspend fun works():List<Work> = withContext(Dispatchers.IO){
        val c=URL(SiteAccess.authenticated("$base/obras.json")).openConnection() as HttpURLConnection
        c.connectTimeout=15000;c.readTimeout=25000
        val text=c.inputStream.bufferedReader().use{it.readText()};c.disconnect()
        val root=if(text.trim()=="null"||text.isBlank())JSONObject()else JSONObject(text)
        root.keys().asSequence().mapNotNull{id->root.optJSONObject(id)?.takeIf{it.optBoolean("publicado",true)&&it.optBoolean("published",true)}?.let{it.toWork(id)}}.filter{it.title.isNotBlank()}.sortedByDescending{it.updatedAt}.toList()
    }
    suspend fun recentUpdates(works:List<Work>,limit:Int=4):List<RecentUpdate>{
        val candidates=works.distinctBy{it.id};val requests=Semaphore(6)
        return coroutineScope { candidates.map { work -> async {
            requests.withPermit { runCatching { chapters(work.id).filter { it.available }.maxByOrNull { ChapterMetadata.latest(it) }?.let { RecentUpdate(work,it,ChapterMetadata.latest(it)) } }.getOrNull() }
        } }.awaitAll().filterNotNull().sortedByDescending { it.updatedAt }.take(limit) }

    }
    suspend fun chapters(workId:String):List<Chapter> = withContext(Dispatchers.IO){
        val c=URL(SiteAccess.authenticated("$base/capitulos/$workId.json")).openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=25000
        val text=c.inputStream.bufferedReader().use{it.readText()};c.disconnect();val root=if(text.trim()=="null"||text.isBlank())JSONObject()else JSONObject(text)
        root.keys().asSequence().mapNotNull{id->root.optJSONObject(id)?.let{ChapterMetadata.parse(id,it)}}.filter{it.published&&it.publicationMode!="draft"}.sortedByDescending{it.number?:-1.0}.toList()
    }
    suspend fun pages(workId:String,chapterId:String):List<String> = withContext(Dispatchers.IO){
        val chapter=chapters(workId).firstOrNull{it.id==chapterId}?:error("Este capítulo ainda não foi publicado.")
        check(chapter.available){"Capítulo agendado. Aguarde a data de liberação."}
        val c=URL(SiteAccess.authenticated("$base/capitulosPaginas/$workId/$chapterId.json")).openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=30000
        val text=c.inputStream.bufferedReader().use{it.readText()};c.disconnect()
        if(text.isNotBlank()&&text.trim()!="null"){
            val decoded=PageManifest.parse(text)
            if(decoded.isNotEmpty())return@withContext decoded
        }
        val legacy=URL(SiteAccess.authenticated("$base/capitulos/$workId/$chapterId.json")).openConnection() as HttpURLConnection
        legacy.connectTimeout=15000;legacy.readTimeout=30000
        val raw=try{JSONObject(legacy.inputStream.bufferedReader().use{it.readText()})}finally{legacy.disconnect()}
        listOf("paginas","pages","imagens","images").firstNotNullOfOrNull{field->raw.opt(field)?.takeIf{it!=JSONObject.NULL}?.let{PageManifest.parse(it.toString()).takeIf(List<String>::isNotEmpty)}}?:emptyList()


    }
    private fun JSONObject.toWork(id:String)=Work(id,string("nome","name","titulo"),string("sinopse","synopsis"),string("capa","cover","coverURL"),string("banner","bannerURL"),string("tipo","type"),string("status"),string("autor","author"),strings(opt("generos")?:opt("genres")),long("atualizadoEm","updatedAt"),long("cliques","leituras","reads"),string("subtitulo","nomeAlternativo","tituloAlternativo","alternateTitle","altName"),string("artista","artist"),string("ano","year"),string("scan","scanName"),string("hospedagem","hosting"),string("idioma","language").ifBlank{"Português"},schedule(optJSONObject("agendaAtualizacao")?:optJSONObject("updateSchedule")))
    private fun schedule(value:JSONObject?):String{val s=value?:return "Sem dia fixo";return when(s.string("tipo","type")){"weekly"->{val raw=s.opt("dias");val days=when(raw){is JSONArray->(0 until raw.length()).map{raw.optInt(it)};is JSONObject->raw.keys().asSequence().filter{raw.optBoolean(it)}.mapNotNull{it.toIntOrNull()}.toList();else->emptyList()}.distinct().sorted();val names=listOf("domingo","segunda-feira","terça-feira","quarta-feira","quinta-feira","sexta-feira","sábado");when(days.size){0->"Sem dia fixo";1->if(days[0] in 0..6)"Toda ${names[days[0]]}" else "Sem dia fixo";else->"Atualiza "+days.mapNotNull{names.getOrNull(it)}.joinToString(" e ")}};"monthly"->"Todo dia ${s.optInt("diaMes",s.optInt("dayOfMonth",1))} de cada mês";else->"Sem dia fixo"}}
    private fun JSONObject.string(vararg k:String)=k.firstNotNullOfOrNull{optString(it).trim().takeIf(String::isNotBlank)}?:""
    private fun JSONObject.long(vararg k:String)=k.firstNotNullOfOrNull{opt(it)?.toString()?.toLongOrNull()}?:0L
    private fun strings(v:Any?):List<String> = when(v){is JSONArray->(0 until v.length()).mapNotNull{v.optString(it).takeIf(String::isNotBlank)};is JSONObject->v.keys().asSequence().mapNotNull{v.optString(it).takeIf(String::isNotBlank)}.toList();else->v?.toString()?.split(',')?.map(String::trim)?.filter(String::isNotBlank)?:emptyList()}
}
