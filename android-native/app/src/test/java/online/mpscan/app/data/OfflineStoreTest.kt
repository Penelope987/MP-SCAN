package online.mpscan.app.data
import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.net.ServerSocket
import kotlin.concurrent.thread
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OfflineStoreTest {
 private val work=Work("test-work","Teste","","","","","","",emptyList(),0,0,adult=true)
 private val chapter=Chapter("chapter-1",1.0,"",true,0)
 private lateinit var store:OfflineStore
 private lateinit var image:ByteArray
 private lateinit var inline:String
 @Before fun setup(){val context=RuntimeEnvironment.getApplication();File(context.filesDir,"mp_scan_downloads").deleteRecursively();store=OfflineStore(context);val out=java.io.ByteArrayOutputStream();Bitmap.createBitmap(8,16,Bitmap.Config.ARGB_8888).apply{eraseColor(android.graphics.Color.RED)}.compress(Bitmap.CompressFormat.PNG,100,out);image=out.toByteArray();inline="data:image/png;base64,"+Base64.encodeToString(image,Base64.NO_WRAP)}
 @Test fun allPagesRemainReadableAfterStoreRecreation()=runBlocking{store.download(work,chapter,List(6){inline},{ });val recreated=OfflineStore(RuntimeEnvironment.getApplication());val pages=recreated.verifiedLocalPages(work.id,chapter.id);assertEquals(6,pages.size);assertTrue(pages.all{File(java.net.URI(it)).readBytes().contentEquals(image)});assertTrue(recreated.downloads().single().adult)}
 @Test fun failedReplacementPreservesPreviouslySavedChapter()=runBlocking{store.download(work,chapter,listOf(inline),{});try{store.download(work,chapter,listOf("data:image/png;base64,"+Base64.encodeToString("invalid".toByteArray(),Base64.NO_WRAP)),{},true);fail("Must reject invalid image")}catch(expected:java.io.IOException){};assertEquals(1,store.verifiedLocalPages(work.id,chapter.id).size);assertEquals(1,store.downloads().size)}
 @Test fun corruptedImageWithSameLengthIsRejected()=runBlocking{store.download(work,chapter,listOf(inline),{});val file=File(java.net.URI(store.localPages(work.id,chapter.id).single()));val bytes=file.readBytes();bytes[bytes.lastIndex]=(bytes.last().toInt() xor 1).toByte();file.writeBytes(bytes);assertTrue(store.verifiedLocalPages(work.id,chapter.id).isEmpty())}
 @Test fun temporaryHttpFailureRetriesAndDownloadsEveryPage()=runBlocking{
  val calls=AtomicInteger();val server=ServerSocket(0,10,java.net.InetAddress.getByName("127.0.0.1"))
  val responder=thread(isDaemon=true){while(!server.isClosed){try{server.accept().use{socket->socket.soTimeout=5000;val reader=socket.getInputStream().bufferedReader();while(true){val line=reader.readLine()?:break;if(line.isEmpty())break};val first=calls.incrementAndGet()==1;val body=if(first)ByteArray(0)else image;val header="HTTP/1.1 "+(if(first)"503 Service Unavailable"else"200 OK")+"\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n";socket.getOutputStream().apply{write(header.toByteArray());write(body);flush()}}}catch(e:java.net.SocketException){if(!server.isClosed)throw e}}}
  try{val url="http://127.0.0.1:${server.localPort}/page";store.download(work,chapter,listOf(url,url,url),{});assertEquals(3,store.verifiedLocalPages(work.id,chapter.id).size);assertEquals(4,calls.get())}finally{server.close();responder.join(1000)}
 }

 @Test fun incompleteDownloadsAreNotListed()=runBlocking{try{store.download(work,chapter,listOf(inline,"data:image/png;base64,aW52YWxpZA=="),{});fail("Must reject partial chapter")}catch(expected:java.io.IOException){};assertTrue(store.downloads().isEmpty());assertTrue(store.localPages(work.id,chapter.id).isEmpty())}
 @Test fun concurrentDownloadsPublishOneCompleteChapter()=runBlocking{coroutineScope{List(3){async{store.download(work,chapter,List(4){inline},{})}}.awaitAll()};assertEquals(4,store.verifiedLocalPages(work.id,chapter.id).size);assertEquals(1,store.downloads().size)}
 @Test fun cancelledReplacementPreservesSavedPages()=runBlocking{store.download(work,chapter,listOf(inline),{});val job=launch{store.download(work,chapter,List(8){inline},{throw CancellationException("cancel")},true)};job.join();assertEquals(1,store.verifiedLocalPages(work.id,chapter.id).size);assertEquals(1,store.downloads().size)}
 @Test fun interruptedPublishRestoresBackup()=runBlocking{store.download(work,chapter,listOf(inline),{});val folder=File(RuntimeEnvironment.getApplication().filesDir,"mp_scan_downloads/test-work/chapter-1");assertTrue(folder.renameTo(File(folder.parentFile,"chapter-1_backup")));assertEquals(1,store.downloads().size);assertEquals(1,store.verifiedLocalPages(work.id,chapter.id).size)}
 @Test fun coverAndGenresAreAvailableWithoutNetwork()=runBlocking{val covered=work.copy(cover=inline,type="Manhwa",genres=listOf("Romance"));store.download(covered,chapter,listOf(inline),{});val saved=store.downloads().single();assertTrue(saved.workCover.startsWith("file:"));assertTrue(File(java.net.URI(saved.workCover)).readBytes().contentEquals(image));assertEquals(listOf("Romance"),saved.genres);assertEquals("Manhwa",saved.type)}
 @Test fun oldDownloadsGainLocalCoversWithoutReplacingPages()=runBlocking{store.download(work,chapter,listOf(inline),{});val metadata=File(RuntimeEnvironment.getApplication().filesDir,"mp_scan_downloads/test-work/chapter-1/chapter.json");val record=org.json.JSONObject(metadata.readText()).put("workCover",inline);metadata.writeText(record.toString());store.cacheMissingCovers();val restored=store.downloads().single();assertTrue(restored.workCover.startsWith("file:"));assertTrue(File(java.net.URI(restored.workCover)).exists());assertEquals(1,store.verifiedLocalPages(work.id,chapter.id).size)}
}
