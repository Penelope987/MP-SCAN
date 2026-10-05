package online.mpscan.app.data

import android.content.Context
import android.graphics.*
import android.util.Base64
import android.util.Base64InputStream
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONObject
import java.io.*
import java.net.*
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

data class PageAsset(val file:File,val width:Int,val height:Int,val regions:Boolean)
object PageFiles {
 private val locks=ConcurrentHashMap<String,Mutex>()
 private val decoding=Semaphore(2)
 private fun hash(source:String)=MessageDigest.getInstance("SHA-256").digest(source.toByteArray()).joinToString(""){"%02x".format(it)}
 suspend fun fetch(context:Context,source:String):PageAsset=withContext(Dispatchers.IO){
  if(source.startsWith("file:"))return@withContext inspect(File(URI(source)))
  val key=hash(source)
  locks.getOrPut(key){Mutex()}.withLock{
   val folder=File(context.cacheDir,"reader-pages").apply{mkdirs()};val file=File(folder,"$key.img")
   if(file.isFile)runCatching{inspect(file)}.getOrNull()?.let{file.setLastModified(System.currentTimeMillis());return@withLock it}
   writeVerified(source,file)
   val asset=inspect(file)
   prune(folder,file)
   asset
  }
 }
 suspend fun writeVerified(source:String,target:File)=withContext(Dispatchers.IO){
  var last:Exception?=null
  repeat(3){attempt->
   currentCoroutineContext().ensureActive()
   val temporary=File(target.parentFile,"${target.name}.${java.util.UUID.randomUUID()}.pending")
   try{
    writeSource(source,temporary)
    inspect(temporary)
    check(temporary.renameTo(target)){"Não foi possível salvar a página."}
    return@withContext
   }catch(e:CancellationException){throw e}catch(e:Exception){last=e}finally{temporary.delete()}
   if(attempt<2)delay(400L*(attempt+1))
  }
  throw IOException("Não foi possível carregar uma das páginas. Confira a conexão.",last)
 }
 private fun writeSource(source:String,target:File){
  if(source.startsWith("mpscan-page:")){
   val path=source.removePrefix("mpscan-page:").substringBefore('?')
   require(Regex("capitulosPaginas/[^/#?]+/[^/#?]+/[^/#?]+").matches(path))
   val c=URL(SiteAccess.authenticated("https://nnnsss-23f2f-default-rtdb.firebaseio.com/$path.json")).openConnection() as HttpURLConnection
   c.connectTimeout=20000;c.readTimeout=90000
   val raw=try{if(c.responseCode !in 200..299)throw IOException("Não foi possível carregar a página.");c.inputStream.bufferedReader().use{it.readText()}}finally{c.disconnect()}
   val page=PageManifest.parse(raw).singleOrNull()?:throw IOException("A página não está disponível.")
   writeSource(page,target);return
  }
  if(source.startsWith("data:image/",true)){
   val comma=source.indexOf(',');require(comma>0)
   target.outputStream().use{out->
    if(source.substring(0,comma).contains(";base64",true))Base64InputStream(StringBytes(source,comma+1),Base64.DEFAULT).use{it.copyTo(out)}
    else out.write(URLDecoder.decode(source.substring(comma+1).replace("+","%2B"),"UTF-8").toByteArray(Charsets.ISO_8859_1))
   }
  }else PageTransport().download(PageManifest.normalize(source)?:throw IOException("A imagem não está disponível."),target)
 }
 fun digest(file:File):String{val digest=MessageDigest.getInstance("SHA-256");file.inputStream().use{input->val buffer=ByteArray(32768);while(true){val count=input.read(buffer);if(count<0)break;digest.update(buffer,0,count)}};return digest.digest().joinToString(""){"%02x".format(it)}}
 fun inspect(file:File):PageAsset {
  if(!file.isFile||file.length()==0L)throw IOException("O arquivo da página está incompleto.")
  val metadata=File(file.parentFile,"chapter.json")
  if(metadata.isFile){val value=JSONObject(metadata.readText());val names=value.optJSONArray("files");val index=names?.let{(0 until it.length()).firstOrNull{index->it.optString(index)==file.name}};val expected=index?.let{value.optJSONArray("hashes")?.optString(it)}.orEmpty();if(expected.isNotBlank()&&digest(file)!=expected)throw IOException("O arquivo da página ficou incompleto.")}
  val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(file.absolutePath,bounds)
  if(bounds.outWidth<=0||bounds.outHeight<=0)throw IOException("O arquivo da página não contém uma imagem válida.")
  val regions=runCatching{@Suppress("DEPRECATION") val decoder=BitmapRegionDecoder.newInstance(file.absolutePath,false);try{decoder!=null}finally{decoder?.recycle()}}.getOrDefault(false)
  val asset=PageAsset(file,bounds.outWidth,bounds.outHeight,regions)
  // Decode both ends: a nonempty file/header alone does not prove a complete download.
  for(tile in listOf(PageTiles.Tile(0,minOf(512,asset.height)),PageTiles.Tile(maxOf(0,asset.height-512),asset.height)).distinct()){
   decode(asset,tile,256).recycle()
  }
  return asset
 }
 suspend fun bitmap(asset:PageAsset,tile:PageTiles.Tile,targetWidth:Int):Bitmap=withContext(Dispatchers.IO){decoding.withPermit{decode(asset,tile,targetWidth)}}
 private fun decode(asset:PageAsset,tile:PageTiles.Tile,targetWidth:Int):Bitmap {
  val options=BitmapFactory.Options().apply{inPreferredConfig=Bitmap.Config.RGB_565;inSampleSize=PageTiles.sample(asset.width,if(asset.regions)tile.height else asset.height,targetWidth)}
  if(asset.regions){
   @Suppress("DEPRECATION") val decoder=BitmapRegionDecoder.newInstance(asset.file.absolutePath,false)?:throw IOException("Não foi possível abrir a imagem.")
   try{return decoder.decodeRegion(Rect(0,tile.top,asset.width,tile.bottom),options)?:throw IOException("A página ficou incompleta.")}finally{decoder.recycle()}
  }
  val full=BitmapFactory.decodeFile(asset.file.absolutePath,options)?:throw IOException("Não foi possível abrir a imagem.")
  val top=(tile.top.toLong()*full.height/asset.height).toInt().coerceIn(0,full.height-1)
  val bottom=(tile.bottom.toLong()*full.height/asset.height).toInt().coerceIn(top+1,full.height)
  val region=Bitmap.createBitmap(full,0,top,full.width,bottom-top)
  if(region!==full)full.recycle();return region
 }
 private fun prune(folder:File,keep:File){
  val files=folder.listFiles()?.filter{it.extension=="img"}?.sortedBy{it.lastModified()}.orEmpty();var bytes=files.sumOf{it.length()}
  for(file in files){if(bytes<=300L*1024*1024)break;if(file!=keep&&locks[file.nameWithoutExtension]?.isLocked!=true){val size=file.length();if(file.delete())bytes-=size}}
 }
 private class StringBytes(private val text:String,private var position:Int):InputStream(){
  override fun read()=if(position<text.length)text[position++].code else -1
  override fun read(bytes:ByteArray,offset:Int,length:Int):Int{if(length==0)return 0;if(position>=text.length)return -1;val count=minOf(length,text.length-position);repeat(count){bytes[offset+it]=text[position++].code.toByte()};return count}
 }
}
