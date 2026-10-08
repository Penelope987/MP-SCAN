package online.mpscan.app.data

import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ChapterRepositoryTest {
 private fun pages(separate:String,shallow:String="null"):List<String> {
  val server=ServerSocket(0,8,java.net.InetAddress.getByName("127.0.0.1"))
  val worker=thread(isDaemon=true){
   try{while(!server.isClosed){server.accept().use{socket->
    val input=socket.getInputStream().bufferedReader()
    val request=input.readLine().orEmpty().split(' ').getOrElse(1){""}
    while(!input.readLine().isNullOrEmpty()){}
    val value=if(request.substringBefore('?')=="/capitulos/work/chapter.json")"""{"publicado":true,"textoNovel":"Uma história escrita."}"""else if(request.substringAfter('?',"")=="shallow=true")shallow else separate
    val bytes=value.toByteArray()
    socket.getOutputStream().apply{write("HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray());write(bytes);flush()}
   }}}catch(e:java.net.SocketException){if(!server.isClosed)throw e}
  }
  try{return runBlocking{CatalogRepository("http://127.0.0.1:${server.localPort}").pages("work","chapter")}}finally{server.close();worker.join(1000)}
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
