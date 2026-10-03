package online.mpscan.app.data

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

data class OfflineChapter(
    val workId: String,
    val workTitle: String,
    val workCover: String,
    val chapterId: String,
    val chapterLabel: String,
    val pageCount: Int,
    val adult: Boolean = false,
    val type:String="",val status:String="",val author:String="",val genres:List<String> = emptyList()
)

class OfflineStore(context: Context) {
    companion object { private val downloadLock = Mutex(); private val fileLock=Any() }
    private val root = File(context.filesDir, "mp_scan_downloads")

    fun localPages(workId: String, chapterId: String): List<String> = synchronized(fileLock) {
        val folder = chapterFolder(workId, chapterId)
        val backup=File(folder.parentFile,folder.name+"_backup");if(!folder.exists()&&backup.exists())backup.renameTo(folder)
        val metadata = File(folder, "chapter.json")
        if (!metadata.isFile) return@synchronized emptyList()
        runCatching {
            val record=JSONObject(metadata.readText());val sizes=record.optJSONObject("sizes")
            val files = record.optJSONArray("files") ?: JSONArray()
            (0 until files.length()).mapNotNull { index ->
                File(folder, files.optString(index)).takeIf { it.isFile && it.length()>0 && (sizes?.optLong(files.optString(index),it.length())?:it.length())==it.length() }?.toURI()?.toString()
            }.takeIf { it.size == files.length() } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun downloads(): List<OfflineChapter> = synchronized(fileLock) {
        root.listFiles()?.filter(File::isDirectory)?.forEach{work->work.listFiles()?.filter{it.isDirectory&&it.name.endsWith("_backup")}?.forEach{backup->val target=File(work,backup.name.removeSuffix("_backup"));if(!target.exists())backup.renameTo(target)}}
        root.listFiles()
        ?.filter(File::isDirectory)
        ?.flatMap { workFolder ->
            workFolder.listFiles()?.filter { it.isDirectory && !it.name.endsWith("_download") && !it.name.endsWith("_backup") }?.mapNotNull { chapterFolder ->
                runCatching {
                    val metadata = JSONObject(File(chapterFolder, "chapter.json").readText())
                    val files = metadata.optJSONArray("files") ?: JSONArray()
                    if (files.length() == 0 || (0 until files.length()).any { !File(chapterFolder, files.getString(it)).let{file->file.isFile&&file.length()>0} }) return@runCatching null
                    OfflineChapter(
                        workId = metadata.getString("workId"),
                        workTitle = metadata.optString("workTitle", "Obra baixada"),
                        workCover = metadata.optString("workCover"),
                        chapterId = metadata.getString("chapterId"),
                        chapterLabel = metadata.optString("chapterLabel", "Capítulo"),
                        pageCount = files.length(), adult = metadata.optBoolean("adult",false),type=metadata.optString("type"),status=metadata.optString("status"),author=metadata.optString("author"),genres=metadata.optJSONArray("genres")?.let{x->(0 until x.length()).map{x.optString(it)}}?:emptyList()
                    )
                }.getOrNull()
            } ?: emptyList()
        }
        ?.sortedWith(compareBy<OfflineChapter> { it.workTitle.lowercase() }.thenBy { it.chapterLabel })
        ?: emptyList()
    }

    suspend fun delete(workId: String, chapterId: String) = withContext(Dispatchers.IO){downloadLock.withLock {
        synchronized(fileLock){chapterFolder(workId, chapterId).deleteRecursively();File(root,safe(workId)).takeIf{it.listFiles().isNullOrEmpty()}?.delete()}
    }}

    suspend fun download(
        work: Work,
        chapter: Chapter,
        pageUrls: List<String>,
        onProgress: (Int) -> Unit,
        replaceExisting: Boolean = false
    ): List<String> = withContext(Dispatchers.IO) { downloadLock.withLock {
        val saved = localPages(work.id, chapter.id)
        if (saved.isNotEmpty() && !replaceExisting) return@withLock saved
        require(pageUrls.isNotEmpty()) { "Capítulo sem páginas" }
        val destination = chapterFolder(work.id, chapter.id)
        val temporary = File(destination.parentFile, destination.name + "_download")
        temporary.deleteRecursively()
        check(temporary.mkdirs()) { "Não foi possível preparar o download" }
        try {
            val completed = AtomicInteger(0)
            val limiter = Semaphore(4)
            val names = coroutineScope {
                pageUrls.mapIndexed { index, source ->
                    async(Dispatchers.IO) {
                        limiter.withPermit {
                            val extension = extensionFor(source)
                            val name = "%04d.%s".format(index + 1, extension)
                            fetchPage(source,File(temporary,name),index+1)
                            onProgress((completed.incrementAndGet() * 100) / pageUrls.size)
                            name
                        }
                    }
                }.awaitAll()
            }
            var localCover=work.cover
            if(work.cover.isNotBlank())try{fetchPage(work.cover,File(temporary,"cover.jpg"),0);localCover=File(destination,"cover.jpg").toURI().toString()}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){File(temporary,"cover.jpg").delete()}
            currentCoroutineContext().ensureActive()
            File(temporary, "chapter.json").writeText(
                JSONObject()
                    .put("workId", work.id)
                    .put("workTitle", work.title)
                    .put("workCover",localCover).put("adult",work.adult).put("type",work.type).put("status",work.status).put("author",work.author).put("genres",JSONArray(work.genres))
                    .put("chapterId", chapter.id)
                    .put("chapterLabel", chapter.label)
                    .put("files", JSONArray(names))
                    .put("sizes",JSONObject().also{x->names.forEach{x.put(it,File(temporary,it).length())}})
                    .put("sha256",JSONObject().also{x->names.forEach{x.put(it,digest(File(temporary,it)))}})
                    .toString()
            )
            synchronized(fileLock){
            val backup=File(destination.parentFile,destination.name+"_backup")
            backup.deleteRecursively()
            if(destination.exists())check(destination.renameTo(backup)){"Não foi possível preservar o capítulo anterior"}
            if(!temporary.renameTo(destination)){backup.renameTo(destination);throw IOException("Não foi possível finalizar o download")}
            backup.deleteRecursively()
            }
            localPages(work.id, chapter.id)
        } catch (error: Throwable) {
            temporary.deleteRecursively()
            throw error
        }
    }

    }

    private fun chapterFolder(workId: String, chapterId: String) =
        File(File(root, safe(workId)), safe(chapterId))

    private fun safe(value: String) = value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun digest(file:File):String {val hash=java.security.MessageDigest.getInstance("SHA-256");file.inputStream().use{input->val buffer=ByteArray(8192);while(true){val count=input.read(buffer);if(count<0)break;hash.update(buffer,0,count)}};return hash.digest().joinToString(""){"%02x".format(it)}}
    suspend fun verifiedLocalPages(workId:String,chapterId:String):List<String> = withContext(Dispatchers.IO){
        runCatching {
        val pages=localPages(workId,chapterId);if(pages.isEmpty())return@runCatching emptyList()
        val folder=chapterFolder(workId,chapterId);val hashes=runCatching{JSONObject(File(folder,"chapter.json").readText()).optJSONObject("sha256")}.getOrNull()
        if(pages.any{source->val file=File(java.net.URI(source));val bounds=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true};android.graphics.BitmapFactory.decodeFile(file.absolutePath,bounds);bounds.outWidth<=0||bounds.outHeight<=0||(hashes?.optString(file.name).orEmpty().let{it.isNotBlank()&&it!=digest(file)})})emptyList()else pages
        }.getOrDefault(emptyList())
    }
    private suspend fun fetchPage(source:String,target:File,page:Int){
        repeat(3){attempt->currentCoroutineContext().ensureActive();try{writePage(source,target);val bounds=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true};android.graphics.BitmapFactory.decodeFile(target.absolutePath,bounds);if(bounds.outWidth<=0||bounds.outHeight<=0)throw IOException("A página $page não contém uma imagem válida.");return}catch(e:IOException){target.delete();if(attempt==2||source.startsWith("data:"))throw e;delay(500L*(attempt+1))}}
    }
    private suspend fun copyPage(input:java.io.InputStream,output:java.io.OutputStream){val buffer=ByteArray(8192);while(true){currentCoroutineContext().ensureActive();val count=input.read(buffer);if(count<0)break;output.write(buffer,0,count)}}
    private suspend fun writePage(source: String, target: File) {
        if(source.startsWith("file:")){File(java.net.URI(source)).inputStream().use{input->target.outputStream().use{copyPage(input,it)}};return}
        if (source.startsWith("data:", ignoreCase = true)) {
            val comma = source.indexOf(',')
            require(comma > 0) { "Imagem inválida" }
            target.outputStream().use { output ->
                android.util.Base64InputStream(source.substring(comma + 1).byteInputStream(), Base64.DEFAULT).use { copyPage(it,output) }
            }
            if(target.length()==0L)throw IOException("Imagem vazia")
            return
        }
        val connection = URL(source).openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "MP-SCAN-Android")
        try {
            if (connection.responseCode !in 200..299) throw java.io.IOException("Falha ao baixar página (HTTP ${connection.responseCode})")
            target.outputStream().use { output -> connection.inputStream.use { copyPage(it,output) } }
            if(target.length()==0L)throw IOException("Imagem vazia")
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
