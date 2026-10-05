package online.mpscan.app.data

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
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
    val work: Work? = null,
    val chapter: Chapter? = null
)

class OfflineStore(context: Context) {
    companion object { private val downloadLock = Mutex() }
    private val root = File(context.filesDir, "mp_scan_downloads")

    fun localPages(workId: String, chapterId: String): List<String> {
        val folder = chapterFolder(workId, chapterId)
        val backup=File(folder.parentFile,folder.name+"_backup")
        if(!folder.exists()&&backup.exists())backup.renameTo(folder)
        val metadata = File(folder, "chapter.json")
        if (!metadata.isFile) return emptyList()
        return runCatching {
            val value=JSONObject(metadata.readText());val files = value.optJSONArray("files") ?: JSONArray();val sizes=value.optJSONArray("sizes")
            (0 until files.length()).mapNotNull { index ->
                File(folder, files.optString(index)).takeIf { file->val expected=sizes?.optLong(index,0)?:0;file.isFile&&file.length()>0&&(expected<=0||file.length()==expected) }?.toURI()?.toString()
            }.takeIf { it.size == files.length() } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun downloads(): List<OfflineChapter> {
        root.listFiles()?.filter(File::isDirectory)?.forEach{work->work.listFiles()?.filter{it.isDirectory&&it.name.endsWith("_backup")}?.forEach{backup->val target=File(work,backup.name.removeSuffix("_backup"));if(!target.exists())backup.renameTo(target)}}
        return root.listFiles()
        ?.filter(File::isDirectory)
        ?.flatMap { workFolder ->
            workFolder.listFiles()?.filter { it.isDirectory && !it.name.endsWith("_download")&&!it.name.endsWith("_backup") }?.mapNotNull { chapterFolder ->
                runCatching {
                    val metadata = JSONObject(File(chapterFolder, "chapter.json").readText())
                    val files = metadata.optJSONArray("files") ?: JSONArray()
                    if (files.length() == 0 || (0 until files.length()).any { index->val expected=metadata.optJSONArray("sizes")?.optLong(index,0)?:0;!File(chapterFolder, files.getString(index)).let{file->file.isFile&&file.length()>0&&(expected<=0||file.length()==expected)} }) return@runCatching null
                    OfflineChapter(
                        workId = metadata.getString("workId"),
                        workTitle = metadata.optString("workTitle", "Obra baixada"),
                        workCover = File(workFolder,"cover.img").takeIf{it.isFile&&it.length()>0}?.toURI()?.toString()?:metadata.optString("workCover"),
                        chapterId = metadata.getString("chapterId"),
                        chapterLabel = metadata.optString("chapterLabel", "Capítulo"),
                        pageCount = files.length(),
                        work = runCatching{OfflineMetadata.decode(metadata.getJSONObject("work"))}.getOrNull(),
                        chapter = runCatching{OfflineMetadata.chapter(metadata.getJSONObject("chapter"))}.getOrNull()
                    )
                }.getOrNull()
            } ?: emptyList()
        }
        ?.sortedWith(compareBy<OfflineChapter> { it.workTitle.lowercase() }.thenBy { it.chapterLabel })
        ?: emptyList()
    }

    fun offlineWorks(): List<Work> = downloads().distinctBy{it.workId}.map { saved ->
        (saved.work ?: Work(saved.workId,saved.workTitle,"",saved.workCover,"","","","",emptyList(),0,0)).copy(cover=saved.workCover,banner="")
    }

    suspend fun cacheWork(work:Work) = withContext(Dispatchers.IO) { downloadLock.withLock {
        val folder=File(root,safe(work.id))
        if(!folder.isDirectory)return@withLock
        folder.listFiles()?.filter{it.isDirectory&&!it.name.endsWith("_download")&&!it.name.endsWith("_backup")}?.forEach{chapterFolder->
            val metadata=File(chapterFolder,"chapter.json")
            runCatching{
                val value=JSONObject(metadata.readText()).put("work",OfflineMetadata.encode(work))
                val pending=File(chapterFolder,"chapter_pending.json")
                pending.writeText(value.toString());check(pending.renameTo(metadata))
            }
        }
        val cover=File(folder,"cover.img")
        if(!cover.isFile&&work.cover.startsWith("https://"))runCatching{
            val pending=File(folder,"cover_pending.img");writePage(work.cover,pending)
            val bounds=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true}
            android.graphics.BitmapFactory.decodeFile(pending.absolutePath,bounds)
            check(bounds.outWidth>0&&bounds.outHeight>0);check(pending.renameTo(cover))
        }
    }}

    fun delete(workId: String, chapterId: String) {
        chapterFolder(workId, chapterId).deleteRecursively()
        File(root, safe(workId)).takeIf { folder->folder.listFiles()?.none{it.isDirectory}!=false }?.deleteRecursively()
    }

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
                            writePage(source, File(temporary, name))
                            PageFiles.inspect(File(temporary,name))
                            onProgress((completed.incrementAndGet() * 100) / pageUrls.size)
                            name
                        }
                    }
                }.awaitAll()
            }
            currentCoroutineContext().ensureActive()
            File(temporary, "chapter.json").writeText(
                JSONObject()
                    .put("work",OfflineMetadata.encode(work))
                    .put("chapter",OfflineMetadata.encode(chapter))
                    .put("workId", work.id)
                    .put("workTitle", work.title)
                    .put("workCover", work.cover)
                    .put("chapterId", chapter.id)
                    .put("chapterLabel", chapter.label)
                    .put("files", JSONArray(names))
                    .put("sources",JSONArray(pageUrls.map{if(it.startsWith("data:"))""else it}))
                    .put("sizes",JSONArray(names.map{File(temporary,it).length()}))
                    .put("hashes",JSONArray(names.map{PageFiles.digest(File(temporary,it))}))
                    .put("fingerprint",fingerprint(pageUrls))
                    .toString()
            )
            val cover=File(destination.parentFile,"cover.img")
            if(!cover.isFile&&work.cover.isNotBlank())runCatching{
                val pending=File(destination.parentFile,"cover_pending.img")
                writePage(work.cover,pending)
                val bounds=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true}
                android.graphics.BitmapFactory.decodeFile(pending.absolutePath,bounds)
                check(bounds.outWidth>0&&bounds.outHeight>0)
                check(pending.renameTo(cover))
            }
            val backup=File(destination.parentFile,destination.name+"_backup")
            backup.deleteRecursively()
            if(destination.exists())check(destination.renameTo(backup)){"Não foi possível preservar o capítulo salvo."}
            if(!temporary.renameTo(destination)){backup.renameTo(destination);error("Não foi possível finalizar o download.")}
            val complete=localPages(work.id,chapter.id)
            if(complete.size!=names.size){destination.deleteRecursively();backup.renameTo(destination);error("O capítulo ficou incompleto.")}
            backup.deleteRecursively()
            complete
        } catch (error: Throwable) {
            temporary.deleteRecursively()
            throw error
        }
    }

    }

    private fun chapterFolder(workId: String, chapterId: String) =
        File(File(root, safe(workId)), safe(chapterId))

    private fun safe(value: String) = value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private suspend fun writePage(source:String,target:File)=PageFiles.writeVerified(source,target)

    suspend fun restorePage(workId:String,chapterId:String,index:Int,source:String):String=withContext(Dispatchers.IO){downloadLock.withLock{
        val folder=chapterFolder(workId,chapterId);val metadata=File(folder,"chapter.json");val value=JSONObject(metadata.readText());val files=value.getJSONArray("files")
        require(index in 0 until files.length())
        val file=File(folder,files.getString(index));require(file.canonicalFile.parentFile==folder.canonicalFile)
        PageFiles.writeVerified(source,file)
        val sizes=value.optJSONArray("sizes")?:JSONArray();sizes.put(index,file.length());value.put("sizes",sizes)
        val sources=value.optJSONArray("sources")?:JSONArray();sources.put(index,if(source.startsWith("data:"))""else source);value.put("sources",sources)
        val hashes=value.optJSONArray("hashes")?:JSONArray();hashes.put(index,PageFiles.digest(file));value.put("hashes",hashes)
        val pending=File(folder,"chapter_pending.json");pending.writeText(value.toString());check(pending.renameTo(metadata))
        file.toURI().toString()
    }}
    fun matches(workId:String,chapter:Chapter,sources:List<String>):Boolean=runCatching{
        val value=JSONObject(File(chapterFolder(workId,chapter.id),"chapter.json").readText())
        val saved=localPages(workId,chapter.id)
        saved.size==sources.size&&saved.isNotEmpty()&&value.optString("fingerprint")==fingerprint(sources)&&value.optJSONObject("chapter")?.optLong("updatedAt")==chapter.updatedAt
    }.getOrDefault(false)
    private fun fingerprint(sources:List<String>):String{val digest=java.security.MessageDigest.getInstance("SHA-256");sources.forEach{digest.update(it.toByteArray());digest.update(0.toByte())};return digest.digest().joinToString(""){"%02x".format(it)}}

    private fun extensionFor(source: String): String {
        if(source.startsWith(ChapterText.PREFIX))return "txt"
        val value = source.substringBefore(';').substringBefore('?').lowercase()
        return when {
            "png" in value -> "png"
            "webp" in value -> "webp"
            "gif" in value -> "gif"
            else -> "jpg"
        }
    }
}
