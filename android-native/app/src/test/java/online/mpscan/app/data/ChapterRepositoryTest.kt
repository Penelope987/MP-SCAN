package online.mpscan.app.data

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ChapterRepositoryTest {
 private fun pages(separate:String,shallow:String="null"):List<String> {
  val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
  server.createContext("/"){exchange->
   val value=if(exchange.requestURI.path=="/capitulos/work/chapter.json")"""{"publicado":true,"textoNovel":"Uma história escrita."}"""else if(exchange.requestURI.query=="shallow=true")shallow else separate
   val bytes=value.toByteArray();exchange.sendResponseHeaders(200,bytes.size.toLong());exchange.responseBody.use{it.write(bytes)}
  }
  server.start()
  try{return runBlocking{CatalogRepository("http://127.0.0.1:${server.address.port}").pages("work","chapter")}}finally{server.stop(0)}
 }
 @Test fun novelIncludesImagesSavedInTheSeparatePublisherNode(){
  val result=pages("""[{"url":"https://scan.example/1.jpg"},{"tipo":"text","texto":"Uma pausa."},{"url":"https://scan.example/2.jpg"}]""")
  assertEquals(4,result.size);assertEquals("Uma história escrita.",ChapterText.decode(result[0]));assertEquals("https://scan.example/1.jpg",result[1]);assertEquals("Uma pausa.",ChapterText.decode(result[2]));assertEquals("https://scan.example/2.jpg",result[3])
 }
 @Test fun textOnlyNovelDoesNotRequireAnyImage(){assertEquals(listOf("Uma história escrita."),pages("null").map(ChapterText::decode))}
 @Test fun writtenChapterKeepsLazyImageReferencesAndNumericOrder(){
  val result=pages("null","""{"pagina_10":true,"pagina_2":true}""")
  assertEquals(3,result.size);assertEquals("Uma história escrita.",ChapterText.decode(result[0]));assertTrue(result[1].contains("/pagina_2?"));assertTrue(result[2].contains("/pagina_10?"))
 }
}
