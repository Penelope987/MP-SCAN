package online.mpscan.app.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.net.ServerSocket
import kotlin.concurrent.thread
import java.io.File
import java.io.IOException

class ReaderPipelineTest {
 @Test fun longPagesHaveNoGapsAndUseBoundedRegions(){for((w,h) in listOf(720 to 15000,1440 to 27497,1080 to 16604,10000 to 30000)){val tiles=PageTiles.plan(w,h);assertEquals(0,tiles.first().top);assertEquals(h,tiles.last().bottom);assertEquals(h,tiles.sumOf{it.height});tiles.zipWithNext().forEach{assertEquals(it.first.bottom,it.second.top)};tiles.forEach{val sample=PageTiles.sample(w,it.height,1080);assertTrue(w/sample<=3072);assertTrue(w.toLong()*it.height/(sample.toLong()*sample)<=4_000_000)}}}
 @Test fun publisherReferencesLoadInNumericOrderWithoutImagesInManifest(){assertEquals(listOf("mpscan-page:capitulosPaginas/w/c/pagina_000002","mpscan-page:capitulosPaginas/w/c/pagina_000010"),PageManifest.lazyReferences(JSONObject("""{"pagina_000010":true,"pagina_000002":true}"""),"capitulosPaginas/w/c"));assertNull(PageManifest.lazyReferences(JSONObject("""{"dataUrl":true,"ordem":true}"""),"x"))}
 @Test fun sourceAliasesAndNestedLegacyPagesAreNotDropped(){assertEquals(listOf("https://image/1?a=1&b=2","data:image/png;base64,iVBORw0KGgoAA"),PageManifest.parse("""{"pages":[{"page":"//image/1?a=1&amp;b=2"},{"base64":"iVBORw0KGgoAA"}]}"""))}
 @Test fun tiledItemsStillIdentifyTheActualPage(){assertEquals(2,PageTiles.sourceIndex("page:2:15"));assertNull(PageTiles.sourceIndex("footer"))}
 @Test fun redirectedTransfersRetainTheEntireResponseAndRejectFailures(){
  val server=ServerSocket(0);val bytes=ByteArray(20000){(it%251).toByte()}
  val worker=thread(isDaemon=true){repeat(3){server.accept().use{socket->
   val reader=socket.getInputStream().bufferedReader();val path=reader.readLine().split(" ")[1]
   while(!reader.readLine().isNullOrEmpty()){}
   val output=socket.getOutputStream()
   val header=when(path){"/redirect"->"HTTP/1.1 302 Found\r\nLocation: /image\r\nContent-Length: 0\r\n";"/image"->"HTTP/1.1 200 OK\r\nContent-Length: ${bytes.size}\r\n";else->"HTTP/1.1 503 Unavailable\r\nContent-Length: 0\r\n"}
   output.write((header+"Connection: close\r\n\r\n").toByteArray());if(path=="/image")output.write(bytes);output.flush()
  }}}
  val file=File.createTempFile("page-test",".img")
  try{val base="http://127.0.0.1:${server.localPort}";PageTransport(1000,1000).download("$base/redirect",file);assertArrayEquals(bytes,file.readBytes());try{PageTransport(1000,1000).download("$base/unavailable",file);fail("HTTP errors cannot be saved as image bytes")}catch(expected:IOException){}}finally{file.delete();server.close();worker.join(1000)}
 }
}
