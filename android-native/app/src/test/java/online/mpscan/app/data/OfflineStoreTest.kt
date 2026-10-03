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
import com.sun.net.httpserver.HttpServer
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
 @Test fun temporaryHttpFailureRetriesAndDownloadsEveryPage()=runBlocking{val calls=AtomicInteger();val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0);server.createContext("/page"){exchange->if(calls.incrementAndGet()==1){exchange.sendResponseHeaders(503,-1)}else{exchange.sendResponseHeaders(200,image.size.toLong());exchange.responseBody.use{it.write(image)}};exchange.close()};server.start();try{val url="http://127.0.0.1:${server.address.port}/page";store.download(work,chapter,listOf(url,url,url),{});assertEquals(3,store.verifiedLocalPages(work.id,chapter.id).size);assertEquals(4,calls.get())}finally{server.stop(0)}}
 @Test fun incompleteDownloadsAreNotListed()=runBlocking{try{store.download(work,chapter,listOf(inline,"data:image/png;base64,aW52YWxpZA=="),{});fail("Must reject partial chapter")}catch(expected:java.io.IOException){};assertTrue(store.downloads().isEmpty());assertTrue(store.localPages(work.id,chapter.id).isEmpty())}
}
